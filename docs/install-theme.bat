@echo off
@REM
@REM Copyright (c) 2018 Bosch Software Innovations GmbH and others.
@REM
@REM All rights reserved. This program and the accompanying materials
@REM are made available under the terms of the Eclipse Public License v1.0
@REM which accompanies this distribution, and is available at
@REM http://www.eclipse.org/legal/epl-v10.html
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