
# script produces implementor.jar (JAR_FILE)
# next this file can be run with
# java -jar implementor.jar CLASS OUTPUT_FOLDER
#   java -jar implementor.jar info.kgeorgiy.ja.kasatov.implementor.Implementor OUTPUT
#   java -jar implementor.jar info.kgeorgiy.java.advanced.implementor.basic.interfaces.InterfaceWithStaticMethod OUTPUT
# or
# java -jar implementor.jar -jar CLASS OUTPUT_FILE
#   java -jar implementor.jar -jar info.kgeorgiy.ja.kasatov.implementor.Implementor OUTPUT/target.jar
#   java -jar implementor.jar -jar info.kgeorgiy.java.advanced.implementor.basic.interfaces.InterfaceWithStaticMethod OUTPUT/target.jar

readonly COMPILED_PATH=./compiled
readonly JAR_FILE=./implementor.jar
readonly MAIN_CLASS=info.kgeorgiy.ja.kasatov.implementor.Implementor
readonly TESTS=./../java-advanced-2023

# TODO: don't compile all tests

# prepare temporary directory
rm -rf $COMPILED_PATH

# add libraries: hamcrest-core, jsoup, junit, quickcheck
# for (kgeorgiy) implementor and base modules
cp -r $TESTS/lib $COMPILED_PATH

# compile package base
javac -d $COMPILED_PATH/info.kgeorgiy.java.advanced.base \
    --module-path $COMPILED_PATH \
    $(find $TESTS/modules/info.kgeorgiy.java.advanced.base -name "*.java")

# compile package (kgeorgiy) implementor
javac -d $COMPILED_PATH/info.kgeorgiy.java.advanced.implementor \
    --module-path $COMPILED_PATH \
    $(find $TESTS/modules/info.kgeorgiy.java.advanced.implementor -name "*.java")

# compile Implementor.java
javac -d $COMPILED_PATH/java.solutions --module-path $COMPILED_PATH \
    --add-modules java.base \
    java-solutions/info/kgeorgiy/ja/kasatov/implementor/Implementor.java \
    java-solutions/module-info.java

# create MANIFEST.MF
printf "Manifest-Version: 1.0\n" > ./MANIFEST.MF
printf "Main-Class: %s\n" $MAIN_CLASS >> ./MANIFEST.MF

# create .jar
jar --verbose --create --file $JAR_FILE --main-class=$MAIN_CLASS \
    -C $COMPILED_PATH/info.kgeorgiy.java.advanced.base . \
    -C $COMPILED_PATH/info.kgeorgiy.java.advanced.implementor . \
    -C $COMPILED_PATH/java.solutions .

# clear temporary directory
rm -rf $COMPILED_PATH

