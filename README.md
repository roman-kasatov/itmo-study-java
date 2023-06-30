## ITMO-JAVA-ADVANCED
Репозиторий с моими решениями домашних заданий для курса технологии Java (java-advanced) в Университете ИТМО

+ [walk](java-solutions/info/kgeorgiy/ja/kasatov/walk) - рекурсивно обходит директории и находит хеш-суммы файлов алгоритмом SHA-256. Корректно обрабатываются возможные ошибки (как и во всех программах далее)

+ [arrayset](java-solutions/info/kgeorgiy/ja/kasatov/arrayset/ArraySet.java) - класс реализует неизменяемое упорядоченное множество на массиве, имплементирует интерфейс [NavigableSet](https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/util/NavigableSet.html)

+ [student](java-solutions/info/kgeorgiy/ja/kasatov/student/StudentDB.java) - задание подразумевало активное использование лямбда-выражений и потоков. Класс содержит набор функций, производящих различные операции с коллекциями, содержащими специфические объекты, преимущественно с помощью потоков

+ [implementor](java-solutions/info/kgeorgiy/ja/kasatov/implementor/Implementor.java) - генерирация (наивной) реализации классов и интерфейсов. Public ункции сопровождаются документацией в виде Javadoc (как и во всех программах делее). Добавлена функция генерации .jar файла для заданного класса

+ [скрипт](generate_jar.bash) для генерации .jar файла класса Implementor (из предыдущего пункта). Добавляет необходимые библиотеки и пакеты

+ [скрипт](generate_javadoc.bash) для генерации Javadoc класса Implementor

+ [IterativeParallelism](java-solutions/info/kgeorgiy/ja/kasatov/concurrent/IterativeParallelism.java) - параллельная обработка операций над списками без использования Concurrency Utilities. В том числе maximum, minimum, all, any, count, filter, map. Количество потоков можно задавать

+ [ParallelMapper](java-solutions/info/kgeorgiy/ja/kasatov/concurrent/ParallelMapperImpl.java) - распараллеливает вычисление функции от листа различных аргументов без использования Concurrency Utilities

+ [WebCrawler](java-solutions/info/kgeorgiy/ja/kasatov/crawler/WebCrawler.java) - потокобезопасный класс, позволяющий скачать сайт до заданной глубины (функция скачивания одной страницы не реализована). Можно задавать ограничения на количество одновременно работающих потоков для скачивания страниц, количество потоков для выгрузки ссылок из страницы, количество одновременно загружаемых страниц с одного хоста

+ [hello](java-solutions/info/kgeorgiy/ja/kasatov/hello) - классы для клиента и сервера, которые взаиможействуют по протоколу UDP. Клиент отправляет на сервер запросы из нескольких потоков (их кол-во задается), ждет ответа и выводит его. Сервер получает запросы, обрабатывает их в нескольких потоках и отправляет ответы клиентам
