
readonly TESTS=./../java-advanced-2023

CLASS_PATH=$TESTS/lib/junit-4.11.jar\;$TESTS/modules/info.kgeorgiy.java.advanced.implementor\;$TESTS/modules/info.kgeorgiy.java.advanced.base

javadoc -private -d javadoc java-solutions/info/kgeorgiy/ja/kasatov/implementor/Implementor.java \
	$TESTS/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/ImplerException.java \
	--class-path $CLASS_PATH