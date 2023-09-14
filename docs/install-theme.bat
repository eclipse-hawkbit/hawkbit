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

rem This script checks if 'hugo' is installed. Afterwards, the Hugo theme is downloaded.

hugo version
if ERRORLEVEL 1 (
    echo [ERROR] Please install Hugo first before proceeding.
    exit 1
)

echo [INFO] 
echo [INFO] Install Hugo Theme
set HUGO_THEMES=themes\hugo-material-docs

if not exist %HUGO_THEMES%\ (
    git submodule add --force https://github.com/digitalcraftsman/hugo-material-docs.git %HUGO_THEMES%
    echo [INFO] ... done
) else echo [INFO] ... theme already installed in: %HUGO_THEMES%

echo [INFO] 
echo [INFO] Launch the documentation locally by running 'mvn site' (or 'hugo server' in the docs directory),
echo [INFO] and browse to 'http://localhost:1313/hawkbit/'.