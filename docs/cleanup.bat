@echo off
@REM
@REM Copyright (c) 2018 Bosch Software Innovations GmbH and others.
@REM
@REM All rights reserved. This program and the accompanying materials
@REM are made available under the terms of the Eclipse Public License v1.0
@REM which accompanies this distribution, and is available at
@REM http://www.eclipse.org/legal/epl-v10.html
@REM

rem This script is used to clean up the previously generated or downloaded files.

echo [INFO] Remove Hugo Theme
rmdir /Q /S themes resources public
echo [INFO] ... done

echo [INFO]

echo [INFO] Remove generated REST docs
del /Q content\rest-api\*.html
echo [INFO] ... done


