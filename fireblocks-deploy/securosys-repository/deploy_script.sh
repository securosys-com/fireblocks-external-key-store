#!/usr/bin/env bash

set -Eeuo pipefail

#! USAGE - Docker Project
  #- bash securosys-fireblocks-deploy/deploy.sh create-configuration-zip-file [v|l]
  #- bash securosys-fireblocks-deploy/deploy.sh create-downloadlink-file [v|l]
  #- bash securosys-fireblocks-deploy/deploy.sh create-downloadlink-file-docker [v|l]
  #- bash securosys-fireblocks-deploy/deploy.sh create-executable-zip-file [v|l]
  #- bash securosys-fireblocks-deploy/deploy.sh create-zip [v|l]
  #- bash securosys-fireblocks-deploy/deploy.sh push-docker-image-to-repository
  #- bash securosys-fireblocks-deploy/deploy.sh push-zip-to-repository [v|l]
  #- bash securosys-fireblocks-deploy/deploy.sh securosys-docker-repository-login
  #- bash securosys-fireblocks-deploy/deploy.sh securosys-docker-repository-logout
  #! - echo "Release Version Tag:" $PROP_PREFIX.$PROP_POSTFIX
#! END USAGE - Docker Project

# Mandatory variables
: \
    "${PRODUCT_FAMILY_NAME:?}" \
    "${INTERNAL_PREFIX:=}" \
    "${JFROG_ARTIFACTORY_URL:=securosys.jfrog.io}" \
    "${JFROG_REPOSITORY_HUMAN_READABLE_NAME:?}" \
    "${JFROG_REPOSITORY_REAL_NAME:?}" \
    "${JFROG_ROBOT_READER_ACCOUNT:=}" \
    "${JFROG_ROBOT_READER_PASS:=}" \
    "${JFROG_ROBOT_WRITER_ACCOUNT:=}" \
    "${JFROG_ROBOT_WRITER_PASS:=}" \
    "${PROJECT_VERSION_FILEPATH:?}" \
    "${PROJECT_RELEASE_NOTES_FILEPATH:?}" \
    "${PROJECT_CONFIGURATION_DIRECTORYPATH:=}" \
    "${PROJECT_CONFIG_TEMPLATE_FILES:=}" \
    "${PROJECT_EXECUTABLE_TEMPLATE_FILES:=}" \
    "${PROJECT_EXECUTABLE_DIRECTORYPATH:=}" \
    "${DOCKER_IMAGE_NAME:=}" \
    "${DOCKER_IMAGE_BUILD_NAME:=}"

#! VARIABLES
  #! SET THE FOLLOWING CI/CD Pipeline Variables (Git: Settings -> CI/CD -> Variables)
  product_fam_name="${PRODUCT_FAMILY_NAME}"                                     #! "Primus", "CloudsHSM", "Imunes", "PrimusAPI", "Securosys" for overall topics

  #! Jfrog vars
  artifactory_url="${JFROG_ARTIFACTORY_URL}"                                    #! securosys.jfrog.io
  repository_real_name="${INTERNAL_PREFIX}${JFROG_REPOSITORY_REAL_NAME}"        #! securosys-vpd
  repository_hr_name="${JFROG_REPOSITORY_HUMAN_READABLE_NAME}"                  #! VPD
  jfrog_robot_reader_username="${JFROG_ROBOT_READER_ACCOUNT}"                   #! robot.reader.vpd
  jfrog_robot_reader_password="${JFROG_ROBOT_READER_PASS}"                      #! maskable, protectable password, url-compatible
  jfrog_robot_writer_username="${JFROG_ROBOT_WRITER_ACCOUNT}"                   #! robot.writer.vpd
  jfrog_robot_writer_password="${JFROG_ROBOT_WRITER_PASS}"                      #! maskable, protectable password, url-compatible

  #! Project vars
  version_filepath="${PROJECT_VERSION_FILEPATH}"                                #! gradle.properties
  source_releasenotes_filepath="${PROJECT_RELEASE_NOTES_FILEPATH}"              #! etc/release_notes/Release_Notes.md
  configuration_template_directorypath="${PROJECT_CONFIGURATION_DIRECTORYPATH}" #! etc/config_templates/config_templates/
  configuration_template_files="${PROJECT_CONFIG_TEMPLATE_FILES}"               #! docker-compose.yml,config-files/template.yml,config-files/application.yml,config-files/log/logback.xml
  executable_template_files="${PROJECT_EXECUTABLE_TEMPLATE_FILES}"              #! csv-string (apple,banana,...)
  executable_template_directorypath="${PROJECT_EXECUTABLE_DIRECTORYPATH}"       #!

  #! Docker vars
  docker_img_name="${DOCKER_IMAGE_NAME}"
  docker_img_build_name="${artifactory_url}/${repository_real_name}/${docker_img_name}"

#! END VARIABLES

#! Parameters:
#! $1 = key; VERSION
#! sample: VERSION=1.0.0
get_property() {
    PROP_KEY=$1
    #--
    PROPERTIES_FILE=${version_filepath}
    PROP_VALUE=$(grep -Ee "^${PROP_KEY}=" "${PROPERTIES_FILE}" | tail -n1 | cut -d '=' -f2)
    echo "${PROP_VALUE}"
}

#! Parameters:
#! $1 = key; VERSION
#! $2 = value; 1.0.0
#! sample: VERSION=1.0.0
set_property() {
    PROP_KEY=$1
    PROP_VALUE=$2
    #--
    PROPERTIES_FILE=${version_filepath}
    TMP_FILE=$(mktemp)
    PROP_LIST=$(grep -vEe "^${PROP_KEY}=" "${PROPERTIES_FILE}")
    if [[ -n "${PROP_VALUE}" ]]; then
        cat <<EOF > "${TMP_FILE}"
${PROP_LIST}
${PROP_KEY}=${PROP_VALUE}
EOF
    else
        cat <<EOF > "${TMP_FILE}"
${PROP_LIST}
EOF
    fi
    sort "${TMP_FILE}" > "${PROPERTIES_FILE}"
    rm "${TMP_FILE}"
}

version=$(get_property "VERSION")
: "${version:?}" # version cannot be empty

#! DO NOT TOUCH VARS
  tmp_directory="./etc/temp"
  temp_configuration_directorypath="${tmp_directory}/configuration_files"
  temp_executable_directorypath="${tmp_directory}/executable_files"
  mkdir -p "${tmp_directory}"
  real_etc=$(realpath ./etc)

  v="v${version}"
  l="latest"
  use_ver() { [[ "${1:?}" =~ ^v.* ]] && echo "${v}" || \
                      ( [[ "${1:?}" =~ ^l.* ]] && echo "${l}" ) || \
                      (echo "Unknown ${1} expected any of {v,l}"; exit 1); }


  tmp_target_dir() { printf '%s/release/%s/' "${real_etc}" "${1:?}" ; }

  mkdir -p "$(tmp_target_dir "${v}")" "$(tmp_target_dir "${l}")"

  target_prefix=${product_fam_name}_${repository_hr_name}

  target_zip_filename()               { printf '%s-%s.zip'               "${target_prefix}"   "${1:?}" ; }
  target_downloadlink_filename()      { printf '%s-downloadlink-%s.txt'  "${target_prefix,,}" "${1:?}" ; }   #! lower-case enforced
  target_configuration_zip_filename() { printf '%s-configuration-%s.zip' "${target_prefix,,}" "${1:?}" ; }   #! lower-case enforced
  target_executable_zip_filename()    { printf '%s-executable-%s.zip'    "${target_prefix,,}" "${1:?}" ; }   #! lower-case enforced
  target_releasenotes_filename()      { printf '%s-releasenotes-%s.md'   "${target_prefix,,}" "${1:?}" ; }   #! lower-case enforced

  artifactory_base_url_curl() { printf '%s/artifactory/%s/%s' "${artifactory_url}" "${repository_real_name}" "${1:?}" ; }
  artifactory_base_url() { printf 'https://%s' "$(artifactory_base_url_curl "${1:?}")" ; }

  docker_image_full_artifact_name_without_version="${artifactory_url}/${repository_real_name}/${docker_img_name}"
#! END DO NOT TOUCH VARS


#! FUNCTIONS
#! ############################## Project File ##############################
  create-zip(){
    ver=$(use_ver "${1:?}")
    mkdir -p "$(tmp_target_dir "${ver}")"
    dl_file="$(tmp_target_dir "${ver}")/$(target_downloadlink_filename "${ver}")"
    if [[ ! -e "${dl_file}" ]]; then
      echo "Error: File not found - $(target_downloadlink_filename "${ver}")"
      exit 1
    fi
    release_note="$(tmp_target_dir "${ver}")/$(target_releasenotes_filename "${ver}")"
    if [[ -e "${source_releasenotes_filepath}" ]]; then
       cp "${source_releasenotes_filepath}" "${release_note}"
    else
      echo "Error: File not found - ${source_releasenotes_filepath}"
      exit 1
    fi
    tgt_dir="$(tmp_target_dir "${ver}")"
    zip_file="$(target_zip_filename "${ver}")"
    (cd "${tgt_dir}" && find . -type f | zip "${zip_file}" -@)
  }

  #! sample: /vpd/v1.0.0/Securosys-VPD-v1.0.0.zip
  push-zip-to-repository() {
    ver=$(use_ver "${1:?}")
    curl -u "${jfrog_robot_writer_username}:${jfrog_robot_writer_password}" \
    -X PUT "$(artifactory_base_url "${ver}")/$(target_zip_filename "${ver}")" \
    -T "$(tmp_target_dir "${ver}")/$(target_zip_filename "${ver}")"
  }

#! ############################## Configuration File(s) ##############################
  create-configuration-zip-file() {
      ver=$(use_ver "${1:?}")
       # Set IFS to comma to split the string
      IFS=',' read -ra configuration_files <<< "${configuration_template_files}"

      mkdir -p "${temp_configuration_directorypath}/config-files/log"

      for conf_file in "${configuration_files[@]}"; do
          filepath="${configuration_template_directorypath}/${conf_file}"
          if [[ -e "${filepath}" ]]; then
              cp "${filepath}" "${temp_configuration_directorypath}/${conf_file}"
              echo "File ${conf_file} copied successfully."
          else
              echo "Error: File not found - ${filepath}"
              exit 1
          fi
      done

      zip_file="$(tmp_target_dir "${ver}")/$(target_configuration_zip_filename "${ver}")"
      (cd "${temp_configuration_directorypath}" && find . -type f | zip "${zip_file}" -@ )

      if [[ -e "${zip_file}" ]]; then
        echo "Zip-File successfully created at ${zip_file}"
      else
        echo "Error: File not found - ${zip_file}"
        exit 1
      fi
  }

#! ############################## Executable File(s) ##############################
  create-executable-zip-file() {
    ver=$(use_ver "${1:?}")

    # Set IFS to comma to split the string
    IFS=',' read -ra exe_files <<< "${executable_template_files}"

    mkdir -p "${temp_executable_directorypath}"

    for exe_file in "${exe_files[@]@P}"; do
        filepath="${executable_template_directorypath}/${exe_file}"
        if [[ -e "${filepath}" ]]; then
            cp "${filepath}" "${temp_executable_directorypath}/${exe_file}"
            echo "File ${exe_file} copied successfully."
        else
            echo "Error: File not found - ${filepath}"
            exit 1
        fi
    done

    zip_file="$(tmp_target_dir "${ver}")/$(target_executable_zip_filename "${ver}")"
    (cd "${temp_executable_directorypath}" && find . -type f | zip "${zip_file}" -@ )

    if [[ -e "${zip_file}" ]]; then
        echo "Zip-File successfully created at ${zip_file}"
    else
        echo "Error: File not found - ${zip_file}"
        exit 1
    fi
  }

#! ############################## Download Link File ##############################
  create-downloadlink-file(){
      ver=$(use_ver "${1:?}")
      download_link_filepath="$(tmp_target_dir "${ver}")/$(target_downloadlink_filename "${ver}")"
      :> "${download_link_filepath}" #! empty file

      {
          echo "********************************************"
          echo "* Distribution repository pull information *"
          echo "********************************************"
          echo ""
          echo "***  Credentials  ***"
          echo "Username: ${jfrog_robot_reader_username}"
          echo "Password: ${jfrog_robot_reader_password}"
          echo ""
          echo "***  Download  ***"
          echo "$(artifactory_base_url "${ver}")/$(target_zip_filename "${ver}")"
          echo ""
          echo "curl -L -XGET https://${jfrog_robot_reader_username}:${jfrog_robot_reader_password}@$(artifactory_base_url_curl "${ver}")/$(target_zip_filename "${ver}") -o $(target_zip_filename "${ver}")"
      } | tee "${download_link_filepath}"
  }

   create-downloadlink-file-docker(){
      ver=$(use_ver "${1:?}")
      download_link_filepath="$(tmp_target_dir "${ver}")/$(target_downloadlink_filename "${ver}")"
      :> "${download_link_filepath}" #! empty file

      docker_image_and_tagname="${docker_image_full_artifact_name_without_version}:${version}"
      docker_image_latest="${docker_image_full_artifact_name_without_version}:latest"

      create-downloadlink-file "${1}"
      # append docker instructions
      {
          echo ""
          echo "***  Docker Image  ***"
          echo "1) Login to repository use the following command with credentials above."
          echo ""
          echo "docker login ${artifactory_url} -u ${jfrog_robot_reader_username}"
          echo ""
          echo "2) Pull an versioned image using the following command"
          echo ""
          echo "docker pull ${docker_image_and_tagname}"
          echo ""
          echo "2.1) Pull the latest image using the following command"
          echo ""
          echo "docker pull ${docker_image_latest}"
          echo ""
      } | tee -a "${download_link_filepath}"
  }

#! ############################## Docker Functions ##############################
  _with_docker_mount_namespace() {
      USER_ID=$(id -u)
      DOCKER_REAL_DIR=$(getent passwd "${USER_ID}" | cut -d: -f6)/.docker
      DOCKER_TMP_DIR=${tmp_directory}/docker/login
      mkdir -p "${DOCKER_TMP_DIR}" "${DOCKER_REAL_DIR}"
      ls "${DOCKER_TMP_DIR}" "${DOCKER_REAL_DIR}"
      unshare --user --map-root-user --mount -- <<EOF
        mount --bind "${DOCKER_TMP_DIR}" "${DOCKER_REAL_DIR}"
        $@
EOF
  }

  securosys-docker-repository-login() {
    _with_docker_mount_namespace \
        docker login -u="${jfrog_robot_writer_username}" -p="${jfrog_robot_writer_password}" "${artifactory_url}"
  }

  securosys-docker-repository-logout() {
    _with_docker_mount_namespace \
        docker logout "${artifactory_url}"
  }

  push-docker-image-to-repository() {
    #! tagging
    docker tag "${docker_img_build_name}:${version}" "${docker_image_full_artifact_name_without_version}:${version}"
    docker tag "${docker_img_build_name}:${version}" "${docker_image_full_artifact_name_without_version}:latest"

    #! pushing
    _with_docker_mount_namespace \
        docker push "${docker_image_full_artifact_name_without_version}:${version}"
    _with_docker_mount_namespace \
        docker push "${docker_image_full_artifact_name_without_version}:latest"

    #! cleanup environment
    #docker image rm "${docker_img_build_name}:${version}"
    docker image rm "${docker_image_full_artifact_name_without_version}:latest"
    docker image rm "${docker_image_full_artifact_name_without_version}:${version}"
  }

#! END FUNCTIONS

"$@"
