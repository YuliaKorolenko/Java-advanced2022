package info.kgeorgiy.ja.korolenko.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StudentDB implements StudentQuery {

    // Common function for getF
    public <T> List<T> getParameter(List<Student> students, Function<Student, T> function) {
        return students.stream().map(function).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getParameter(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getParameter(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getParameter(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getParameter(students, c -> c.getFirstName() + " " + c.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream().map(Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        // :NOTE: redundant .map(Object::toString)
        return students.stream().max(Comparator.comparingInt(Student::getId))
                .map(Student::getFirstName).map(Object::toString).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        // :NOTE: можно убрать Comparator.comparingInt(Student::getId)
        return students.stream().sorted(Comparator.comparingInt(Student::getId)).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream().sorted(Comparator.comparing(Student::getLastName)
                .thenComparing(Student::getFirstName).reversed()
                .thenComparingInt(Student::getId)).collect(Collectors.toList());
    }

    // K
    private <T> List<Student> findBy(Collection<Student> students, T name, Function<Student, T> function) {
        return sortStudentsByName(students.stream()
                .filter(c -> Objects.equals(function.apply(c), name)).collect(Collectors.toList()));
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findBy(students, name, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findBy(students, name, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findBy(students, group, Student::getGroup);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students.stream().filter(c -> c.getGroup() == group)
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())
                ));
    }
}
