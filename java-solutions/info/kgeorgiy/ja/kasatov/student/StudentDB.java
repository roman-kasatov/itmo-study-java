package info.kgeorgiy.ja.kasatov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.GroupQuery;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class StudentDB implements GroupQuery {

    private static final Comparator<Student> nameComparator = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .reversed()
            .thenComparing(Student::getId);

    /**
     * Returns groups ordered by name with students ordered by comparator
     */
    private List<Group> aggregateByGroup(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(
                        s -> new Group(
                                s.getKey(),
                                s.getValue().stream()
                                        .sorted(comparator)
                                        .collect(Collectors.toCollection(ArrayList::new))))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return aggregateByGroup(students, nameComparator);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return aggregateByGroup(students, Comparator.comparing(Student::getId));
    }

    /**
     * Returns name of group with maximum metric. If there is more than one such group, returns
     * with maximum name if (maxName) and with minimum otherwise
     */
    private GroupName getMaxGroup(Collection<Student> students, Function<List<Student>, Integer> metric, boolean maxName) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet().stream()
                .map(s -> Map.entry(s.getKey(), metric.apply(s.getValue())))
                .max(Comparator.comparing(Map.Entry<GroupName, Integer>::getValue)
                        .thenComparing(
                                Map.Entry<GroupName, Integer>::getKey,
                                maxName ? Comparator.naturalOrder() : Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getMaxGroup(students, List::size, true);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getMaxGroup(
                students,
                l -> (int) l.stream()
                        .map(Student::getFirstName)
                        .distinct()
                        .count(),
                false);
    }

    private <T> List<T> mapStudentsToList(List<Student> list, Function<Student, T> function) {
        return list.stream().map(function).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapStudentsToList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapStudentsToList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return mapStudentsToList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapStudentsToList(students, s -> String.join(" ", s.getFirstName(), s.getLastName()));
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(Comparator.comparing(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    private List<Student> sortStudents(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudents(students, Comparator.comparing(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudents(students, nameComparator);
    }

    /**
     * Returns list of students for which function(student) == target
     */
    private <T> List<Student> filterStudents(Collection<Student> students,
                                             Function<Student, T> function, T target) {
        return students.stream()
                .filter(s -> function.apply(s).equals(target))
                .sorted(nameComparator)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterStudents(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterStudents(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return filterStudents(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students.stream()
                .filter(s -> s.getGroup().equals(group))
                .collect(Collectors.groupingBy(Student::getLastName))
                .entrySet().stream()
                .map(s -> Map.entry(
                        s.getKey(),
                        s.getValue().stream()
                                .map(Student::getFirstName)
                                .min(Comparator.naturalOrder()).orElse("")))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x, HashMap::new));
    }
}
