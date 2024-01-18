@echo off
@REM
@REM Copyright (c) 2023 Bosch.IO GmbH and others
@REM
@REM This program and the accompanying materials are made
@REM available under the terms of the Eclipse Public License 2.0
@REM which is available at https://www.eclipse.org/legal/epl-2.0/
@REM
@REM SPDX-License-Identifier: EPL-2.0
@REM

rem This script is used to clean up the previously generated or downloaded files.

echo [INFO] Remove Hugo Theme
rmdir /Q /S themes resources public
echo [INFO] ... done

echo [INFO]

echo [INFO] Remove generated REST docs
del /Q content\rest-api\*.html
echo [INFO] ... done
