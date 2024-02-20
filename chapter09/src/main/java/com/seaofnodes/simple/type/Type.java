package com.seaofnodes.simple.type;

import java.util.HashMap;

/**
 * These types are part of a Monotone Analysis Framework,
 * @see <a href="https://www.cse.psu.edu/~gxt29/teaching/cse597s21/slides/08monotoneFramework.pdf">see for example this set of slides</a>.
 * <p> 
 * The types form a lattice; @see <a href="https://en.wikipedia.org/wiki/Lattice_(order)">a symmetric complete bounded (ranked) lattice.</a>
 * <p>
 * This wild lattice theory will be needed later to allow us to easily beef up
 * the analysis and optimization of the Simple compiler... but we don't need it
 * now, just know that it is coming along in a later Chapter.
 * <p>g
 * One of the fun things here is that while the theory is deep and subtle, the
 * actual implementation is darn near trivial and is generally really obvious
 * what we're doing with it.  Right now, it's just simple integer math to do
 * simple constant folding e.g. 1+2 == 3 stuff.
 */    

public class Type {
    static final HashMap<Type,Type> INTERN = new HashMap<>();

    // ----------------------------------------------------------
    // Simple types are implemented fully here.  "Simple" means: the code and
    // type hierarchy are simple, not that the Type is conceptually simple.
    static final byte TBOT    = 0; // Bottom (ALL)
    static final byte TTOP    = 1; // Top    (ANY)
    static final byte TCTRL   = 2; // Ctrl flow bottom
    static final byte TXCTRL  = 3; // Ctrl flow top (mini-lattice: any-xctrl-ctrl-all)
    static final byte TSIMPLE = 4; // End of the Simple Types
    static final byte TINT    = 5; // All Integers; see TypeInteger
    static final byte TTUPLE  = 6; // Tuples; finite collections of unrelated Types, kept in parallel

    public final byte _type;

    public boolean is_simple() { return _type < TSIMPLE; }
    private static final String[] STRS = new String[]{"Bot","Top","Ctrl","~Ctrl"};
    protected Type(byte type) { _type = type; }

    public static final Type BOTTOM   = new Type( TBOT   ).intern(); // ALL
    public static final Type TOP      = new Type( TTOP   ).intern(); // ANY
    public static final Type CONTROL  = new Type( TCTRL  ).intern(); // Ctrl
    public static final Type XCONTROL = new Type( TXCTRL ).intern(); // ~Ctrl

    // Strict constant values, things on the lattice centerline.
    // Excludes both high and low values
    public boolean isConstant() { return false; }

    public StringBuilder _print(StringBuilder sb) {return is_simple() ? sb.append(STRS[_type]) : sb;}

    // ----------------------------------------------------------

    // Factory method which interns "this"
    protected <T extends Type> T intern() {
        T nnn = (T)INTERN.get(this);
        if( nnn==null ) 
            INTERN.put(nnn=(T)this,this);
        return nnn;
    }
    
    private int _hash;          // Hash cache; not-zero when set.
    @Override
    public final int hashCode() {
        if( _hash!=0 ) return _hash;
        _hash = hash();
        if( _hash==0 ) _hash = 0xDEADBEEF; // Bad hash from subclass; use some junk thing
        return _hash;
    }
    // Override in subclasses
    int hash() { return _type; }
    
    @Override
    public final boolean equals( Object o ) {
        if( o==this ) return true;
        if( !(o instanceof Type t)) return false;
        if( _type != t._type ) return false;
        return eq(t);
    }
    // Overridden in subclasses; subclass can assume "this!=t" and java classes are same
    boolean eq(Type t) { return true; }
    
    
    // ----------------------------------------------------------
    public final Type meet(Type t) {
        // Shortcut for the self case
        if( t == this ) return this;
        // Same-type is always safe in the subclasses
        if( _type==t._type ) return xmeet(t);
        // Reverse; xmeet 2nd arg is never "is_simple" and never equal to "this".
        if(   is_simple() ) return this.xmeet(t   );
        if( t.is_simple() ) return t   .xmeet(this);
        return BOTTOM;        // Mixing 2 unrelated types
    }

    // Compute meet right now.  Overridden in subclasses.
    // Handle cases where 'this.is_simple()' and unequal to 't'.
    // Subclassed xmeet calls can assert that '!t.is_simple()'.
    protected Type xmeet(Type t) {
        assert is_simple(); // Should be overridden in subclass
        // ANY meet anything is thing; thing meet ALL is ALL
        if( _type==TBOT || t._type==TTOP ) return this;
        if( _type==TTOP || t._type==TBOT ) return    t;
        // 'this' is {TCTRL,TXCTRL}
        if( !t.is_simple() ) return BOTTOM;
        // 't' is {TCTRL,TXCTRL}
        return _type==TCTRL || t._type==TCTRL ? CONTROL : XCONTROL;
    }

    // True if this "isa" t; e.g. 17 isa TypeInteger.BOT
    public boolean isa( Type t ) { return meet(t)==t; }
    
    // ----------------------------------------------------------
    @Override
    public final String toString() {
        return _print(new StringBuilder()).toString();
    }
}
