cd ..

dir /s /B "*.java" > sources.txt
javac -cp ".\..\java-advanced-2022\artifacts\*" @sources.txt -d out/

jar -cfm Implementor.jar java-solutions/info/kgeorgiy/ja/korolenko/implementor/MANIFEST.MF -C out info/kgeorgiy/ja/korolenko/implementor/

del sources.txt