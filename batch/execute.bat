@echo off
if "%RUNNER_ROOT%"=="" goto set_runner_root
set _JAVACMD=java.exe
if exist thirdparty\jdk\bin\java.exe set _JAVACMD=thirdparty\jdk\bin\java.exe
if exist ..\jdk\bin\java.exe set _JAVACMD=..\jdk\bin\java.exe
if exist "%JAVA_HOME%\bin\java.exe" set _JAVACMD=%JAVA_HOME%\bin\java.exe

set CLASS_PATH=scenario-executor.jar;lib/*;%RUNNER_ROOT%/lib/*;%RUNNER_ROOT%/thirdparty/lib/*;%RUNNER_ROOT%/thirdparty/commonLib/*;%RUNNER_ROOT%/thirdparty/ant/lib/*
set MAIN_CLASS=org.jsystemtest.CommandLineInterface
"%_JAVACMD%" -cp "%CLASS_PATH%" "%MAIN_CLASS%" %1 %2 %3 %4 %5 %6 %7
goto quit
:set_runner_root
	echo RUNNER_ROOT environment variable is not set
:quit
	