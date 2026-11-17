#!/bin/bash
# Tạo 4 database tách biệt, mỗi database một user riêng.
# REVOKE CONNECT FROM PUBLIC => user của service này không kết nối được database của service khác.
set -e

create_db() {
  local db="$1" user="$2" pass="$3"
  echo ">> Tạo database $db cho user $user"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<SQL
CREATE USER $user WITH PASSWORD '$pass';
CREATE DATABASE $db OWNER $user;
REVOKE ALL ON DATABASE $db FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE $db TO $user;
SQL
}

create_db eduzone_course_db     course_user     "$COURSE_DB_PASSWORD"
create_db eduzone_student_db    student_user    "$STUDENT_DB_PASSWORD"
create_db eduzone_enrollment_db enrollment_user "$ENROLLMENT_DB_PASSWORD"
create_db eduzone_access_db     access_user     "$ACCESS_DB_PASSWORD"
