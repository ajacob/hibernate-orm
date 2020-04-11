/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh13944;

import org.hibernate.annotations.Cascade;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-13944")
public class HHH13944Test extends BaseCoreFunctionalTestCase {

    @Before
    public void setUp() {
        doInJPA(this::sessionFactory, em -> {
            Skill mathSkill = new Skill();
            Skill frenchSkill = new Skill();

            em.persist(mathSkill);
            em.persist(frenchSkill);

            Teacher t1 = new Teacher();

            Teacher t2 = new Teacher();
            t2.getSkills().add(mathSkill);

            Teacher t3 = new Teacher();
            t3.getSkills().add(mathSkill);
            t3.getSkills().add(frenchSkill);

            em.persist(t1);
            em.persist(t2);
            em.persist(t3);

            Student student = new Student();
            student.setTeacher(t3); // Teacher with 2 skills

            em.persist(student);
        });
    }

    @Test
    public void testSizeWithoutNestedPath() {
        doInJPA(this::sessionFactory, em -> {
            List<Teacher> teachers = em.createQuery("select teacher from Teacher teacher where size(teacher.skills) > 0", Teacher.class).getResultList();
            assertEquals(2, teachers.size());
        });
    }

    @Test
    public void testSizeWithNestedPath() {
        doInJPA(this::sessionFactory, em -> {
            List<Student> students = em.createQuery("select student from Student student where size(student.teacher.skills) > 0", Student.class).getResultList();
            assertEquals(1L, students.size());
        });
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Skill.class, Teacher.class, Student.class };
    }

    @Entity(name = "Skill")
    public static class Skill {

        private Integer id;

        private Set<Teacher> teachers;

        @Id
        @GeneratedValue
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        @ManyToMany(mappedBy = "skills")
        public Set<Teacher> getTeachers() {
            return teachers;
        }

        public void setTeachers(Set<Teacher> teachers) {
            this.teachers = teachers;
        }
    }

    @Entity(name = "Teacher")
    public static class Teacher {

        private Integer id;

        private Set<Student> students = new HashSet<>();

        private Set<Skill> skills = new HashSet<>();

        @Id
        @GeneratedValue
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
        public Set<Student> getStudents() {
            return students;
        }

        public void setStudents(Set<Student> students) {
            this.students = students;
        }

        @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
        @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
        public Set<Skill> getSkills() {
            return skills;
        }

        public void setSkills(Set<Skill> skills) {
            this.skills = skills;
        }
    }

    @Entity(name = "Student")
    public static class Student {

        private Integer id;

        private Teacher teacher;

        @Id
        @GeneratedValue
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        @ManyToOne(optional = false)
        @JoinColumn(name = "teacher_id")
        public Teacher getTeacher() {
            return teacher;
        }

        public void setTeacher(Teacher teacher) {
            this.teacher = teacher;
        }
    }
}
