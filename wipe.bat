call mvn clean
del /F /Q docs.tex docs.tex.map *.jar compilation.log MANIFEST.MF
rd /S /Q out target classes
