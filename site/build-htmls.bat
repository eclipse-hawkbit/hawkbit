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

rem Checking for Redoc CLI and npm
call npx @redocly/cli --version 1> nul 2> nul

if ERRORLEVEL 1 (
    echo [ERROR] Redoc CLI is not installed! Please make suer to install it before trying again.
    exit 1
)

rem Execute the npx command
call npx @redocly/cli build-docs %cd%\content\rest-api\mgmt.yaml -o %cd%\content\rest-api\mgmt.html

if ERRORLEVEL 1 (
    echo [ERROR] Failed to execute the Redoc CLI command form MGMT API.
    exit 1
) else (
    echo [INFO] Successfully executed the Redoc CLI command for MGMT API.
)

rem Execute the npx command
call npx @redocly/cli build-docs %cd%\content\rest-api\ddi.yaml -o %cd%\content\rest-api\ddi.html

if ERRORLEVEL 1 (
    echo [ERROR] Failed to execute the Redoc CLI command form DDI API.
    exit 1
) else (
    echo [INFO] Successfully executed the Redoc CLI command for DDI API.
)