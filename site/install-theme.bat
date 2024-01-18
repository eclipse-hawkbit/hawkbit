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
set CSS_FILE=themes\hugo-material-docs\static\stylesheets\application.css

if not exist %HUGO_THEMES%\ (
    git submodule add --force https://github.com/digitalcraftsman/hugo-material-docs.git %HUGO_THEMES%
    echo [INFO] ... done
) else echo [INFO] ... theme already installed in: %HUGO_THEMES%

rem This script uses 'awk' to replace 1200px with 1500px in the application.css file from 'hugo'
if exist %CSS_FILE%  (
then
    powershell -Command "(gc %CSS_FILE%) -replace 'max-width:1200px', 'max-width:1500px' | Out-File -encoding ASCII %CSS_FILE%"
    echo [INFO] CSS updated content successfully!
else
    echo [WARN] CSS file not found!
fi

echo [INFO] 
echo [INFO] Launch the documentation locally by running 'mvn site' (or 'hugo server' in the docs directory),
echo [INFO] and browse to 'http://localhost:{port}/hawkbit/'.