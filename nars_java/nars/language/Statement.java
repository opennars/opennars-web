/*
 * Statement.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.language;

import nars.core.Parameters;
import nars.inference.TemporalRules;
import nars.io.Symbols.NativeOperator;
import static nars.io.Symbols.NativeOperator.STATEMENT_CLOSER;
import static nars.io.Symbols.NativeOperator.STATEMENT_OPENER;
import nars.io.Texts;

/**
 * A statement is a compound term, consisting of a subject, a predicate, and a
 * relation symbol in between. It can be of either first-order or higher-order.
 */
public abstract class Statement extends CompoundTerm {
    
    /**
     * Constructor with partial values, called by make
     *
     * @param arg The component list of the term
     */
    protected Statement(final CharSequence name, final Term[] arg) {
        super(name, arg);
    }
    
    /**
     * Constructor with full values, called by clone
     *
     * @param n The nameStr of the term
     * @param cs Component list
     * @param con Constant indicator
     * @param i Syntactic complexity of the compound
     */
    @Deprecated protected Statement(final CharSequence n, final Term[] cs, final boolean con, final short i) {
        super(n, cs, con, i);
    }
    
    /**
     * High-performance constructor that avoids recalculating some Term metadata when created
     */
    protected Statement(final CharSequence n, final Term[] cs, final boolean con, final boolean hasVar, final short i) {
        super(n, cs, con, hasVar, i);
    }
    
    /**
     * Make a Statement from given components, called by the rules
     * @return The Statement built
     * @param subj The first component
     * @param pred The second component
     * @param statement A sample statement providing the class type
     */
    public static Statement make(final Statement statement, final Term subj, final Term pred) {        
        if (statement instanceof Inheritance) {
            return Inheritance.make(subj, pred);
        }
        if (statement instanceof Similarity) {
            return Similarity.make(subj, pred);
        }
        if (statement instanceof Implication) {
            return Implication.make(subj, pred, statement.getTemporalOrder());
        }
        if (statement instanceof Equivalence) {
            return Equivalence.make(subj, pred, statement.getTemporalOrder());
        }
        return null;
    }
    
    /**
     * Make a Statement from String, called by StringParser
     *
     * @param o The relation String
     * @param subject The first component
     * @param predicate The second component
     * @param memory Reference to the memory
     * @return The Statement built
     */
    final public static Statement make(final NativeOperator o, final Term subject, final Term predicate) {
        if (invalidStatement(subject, predicate)) {
            return null;
        }
        
        switch (o) {
            case INHERITANCE:
                return Inheritance.make(subject, predicate);
            case SIMILARITY:
                return Similarity.make(subject, predicate);
            case INSTANCE:
                return Instance.make(subject, predicate);
            case PROPERTY:
                return Property.make(subject, predicate);
            case INSTANCE_PROPERTY:
                return InstanceProperty.make(subject, predicate);
            case IMPLICATION:
                return Implication.make(subject, predicate);
            case IMPLICATION_AFTER:
                return Implication.make(subject, predicate, TemporalRules.ORDER_FORWARD);
            case IMPLICATION_BEFORE:
                return Implication.make(subject, predicate, TemporalRules.ORDER_BACKWARD);
            case IMPLICATION_WHEN:
                return Implication.make(subject, predicate, TemporalRules.ORDER_CONCURRENT);
            case EQUIVALENCE:
                return Equivalence.make(subject, predicate);
            case EQUIVALENCE_AFTER:
                return Equivalence.make(subject, predicate, TemporalRules.ORDER_FORWARD);
            case EQUIVALENCE_WHEN:
                return Equivalence.make(subject, predicate, TemporalRules.ORDER_CONCURRENT);            
        }
        
        return null;
    }

    /**
     * Make a Statement from given term, called by the rules
     *
     * @param order The temporal order of the statement
     * @return The Statement built
     * @param subj The first component
     * @param pred The second component
     * @param statement A sample statement providing the class type
     * @param memory Reference to the memory
     */
//    public static Statement make(final Statement statement, final Term subj, final Term pred, final Memory memory) {
//        return make(statement, subj, pred, TemporalRules.ORDER_NONE, memory);
//    }
    
    final public static Statement make(final Statement statement, final Term subj, final Term pred, int order) {

        if (statement instanceof Inheritance) {
            return Inheritance.make(subj, pred);
        }
        if (statement instanceof Similarity) {
            return Similarity.make(subj, pred);
        }
        if (statement instanceof Implication) {
            return Implication.make(subj, pred, order);
        }
        if (statement instanceof Equivalence) {
            return Equivalence.make(subj, pred, order);
        }
        
        throw new RuntimeException("Unrecognized type for Statement.make: " + statement.getClass().getSimpleName() + ", subj=" + subj + ", pred=" + pred + ", order=" + order);        
    }

    /**
     * Make a symmetric Statement from given term and temporal
 information, called by the rules
     *
     * @param statement A sample asymmetric statement providing the class type
     * @param subj The first component
     * @param pred The second component
     * @param order The temporal order
     * @param memory Reference to the memory
     * @return The Statement built
     */
    final public static Statement makeSym(final Statement statement, final Term subj, final Term pred, final int order) {
        if (statement instanceof Inheritance) {
            return Similarity.make(subj, pred);
        }
        if (statement instanceof Implication) {
            return Equivalence.make(subj, pred, order);
        }
        return null;
    }



    /**
     * Override the default in making the nameStr of the current term from
     * existing fields
     *
     * @return the nameStr of the term
     */
    @Override
    protected CharSequence makeName() {
        return makeStatementName(getSubject(), operator(), getPredicate());
    }

    /**
     * Default method to make the nameStr of an image term from given fields
     *
     * @param subject The first component
     * @param predicate The second component
     * @param relation The relation operator
     * @return The nameStr of the term
     */
    final protected static CharSequence makeStatementName(final Term subject, final NativeOperator relation, final Term predicate) {
        final CharSequence subjectName = subject.name();
        final CharSequence predicateName = predicate.name();
        int length = subjectName.length() + predicateName.length() + relation.toString().length() + 4;
        
        StringBuilder sb = new StringBuilder(length)
            .append(STATEMENT_OPENER.ch)
            .append(subjectName)
            .append(' ').append(relation).append(' ')
            .append(predicateName)
            .append(STATEMENT_CLOSER.ch);
            
        //EITHER WORKS, see which is faster.  but using the StringBuilder as-is should save memory:
        return Texts.yarn(Parameters.ROPE_TERMLINK_TERM_SIZE_THRESHOLD, sb);
        //return sb.toString();
        
        
        
    }
    
    /**
     * Check the validity of a potential Statement. [To be refined]
     * <p>
     * @param subject The first component
     * @param predicate The second component
     * @return Whether The Statement is invalid
     */
    final public static boolean invalidStatement(final Term subject, final Term predicate) {
        if (subject.equals(predicate)) {
            return true;
        }
        if (invalidReflexive(subject, predicate)) {
            return true;
        }
        if (invalidReflexive(predicate, subject)) {
            return true;
        }
        if ((subject instanceof Statement) && (predicate instanceof Statement)) {
            final Statement s1 = (Statement) subject;
            final Statement s2 = (Statement) predicate;
            final Term t11 = s1.getSubject();
            final Term t22 = s2.getPredicate();
            final Term t12 = s1.getPredicate();
            final Term t21 = s2.getSubject();
            if (t11.equals(t22) && t12.equals(t21)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if one term is identical to or included in another one, except in a
     * reflexive relation
     * <p>
     * @param t1 The first term
     * @param t2 The second term
     * @return Whether they cannot be related in a statement
     */
    private static boolean invalidReflexive(final Term t1, final Term t2) {
        if (!(t1 instanceof CompoundTerm)) {
            return false;
        }
        final CompoundTerm com = (CompoundTerm) t1;
        if ((com instanceof ImageExt) || (com instanceof ImageInt)) {
            return false;
        }
        return com.containsTerm(t2);
    }

   
    public static boolean invalidPair(final CharSequence s1, final CharSequence s2) {
        boolean s1Indep = Variables.containVarIndep(s1);
        boolean s2Indep = Variables.containVarIndep(s2);
        if (s1Indep && !s2Indep) {
            return true;
        } else if (!s1Indep && s2Indep) {
            return true;
        }
        return false;
    }
    

    /**
     * Check the validity of a potential Statement. [To be refined]
     * <p>
     * Minimum requirement: the two terms cannot be the same, or containing each
     * other as component
     *
     * @return Whether The Statement is invalid
     */
    public boolean invalid() {
        return invalidStatement(getSubject(), getPredicate());
    }
    
 
    /**
     * Return the first component of the statement
     *
     * @return The first component
     */
    public Term getSubject() {
        return term[0];
    }

    /**
     * Return the second component of the statement
     *
     * @return The second component
     */
    public Term getPredicate() {
        return term[1];
    }

    @Override public abstract Statement clone();
   
}