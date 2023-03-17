package info.kgeorgiy.ja.kasatov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.GroupQuery;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;


public class StudentDB implements GroupQuery {

    private static final Comparator<Student> NAME_COMPARATOR = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .reversed()
            .thenComparing(Student::getId);

    /**
     * Returns groups ordered by name with students ordered by comparator
     */
    private List<Group> aggregateByGroup(final Collection<Student> students, final Comparator<Student> comparator) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(
                        entry -> new Group(
                                entry.getKey(),
                                entry.getValue().stream()
                                        .sorted(comparator)
                                        .collect(Collectors.toCollection(ArrayList::new))))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return aggregateByGroup(students, NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return aggregateByGroup(students, Comparator.naturalOrder());
    }

    /**
     * Returns name of group with maximum metric. If there is more than one such group, returns
     * with maximum name if (maxName) and with minimum otherwise
     */
    private <T extends Comparable<? super T>> GroupName getMaxGroup(
            final Collection<Student> students,
            final Collector<Student, ?, T> metric,
            final Comparator<GroupName> groupComparator
    ) {
        return students.stream()
                .collect(Collectors.groupingBy(
                        Student::getGroup,
                        metric
                ))
                .entrySet().stream()
                .max(Map.Entry.<GroupName, T>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(groupComparator)))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Override
    public GroupName getLargestGroup(final Collection<Student> students) {
        return getMaxGroup(
                students,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue),
                Comparator.naturalOrder()
        );
    }

    @Override
    public GroupName getLargestGroupFirstName(final Collection<Student> students) {
        return getMaxGroup(
                students,
                Collectors.mapping(
                        Student::getFirstName,
                        Collectors.collectingAndThen(Collectors.toSet(), Set<String>::size)
                ),
                Comparator.reverseOrder()
        );
    }

    private <T> List<T> mapStudentsToList(final List<Student> list, final Function<Student, T> function) {
        return list.stream().map(function).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return mapStudentsToList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return mapStudentsToList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final List<Student> students) {
        return mapStudentsToList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return mapStudentsToList(students, s -> s.getFirstName() + " " + s.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> students) {
        return students.stream()
                .max(Comparator.comparing(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    private List<Student> sortStudents(final Collection<Student> students, final Comparator<Student> comparator) {
        return students.stream()
                .sorted(comparator)
                .toList();
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return sortStudents(students, Comparator.comparing(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sortStudents(students, NAME_COMPARATOR);
    }

    /**
     * Returns list of students for which function(student) == target
     */
    private static <T> List<Student> filterStudents(
            final Collection<Student> students,
            final Function<Student, T> function, final T target
    ) {
        return students.stream()
                .filter(s -> function.apply(s).equals(target))
                .sorted(NAME_COMPARATOR)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return filterStudents(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return filterStudents(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final GroupName group) {
        return filterStudents(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final GroupName group) {
        return students.stream()
                .filter(s -> s.getGroup().equals(group))
                .collect(Collectors.groupingBy(Student::getLastName))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(Student::getFirstName)
                                .min(Comparator.naturalOrder())
                                .orElse("")));
    }
}
