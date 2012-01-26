Communicating Scala Objects (January 2012)

This is the main public distribution site for the current
version of Communicating Scala Objects. Available here are
source and documentation files

        cso-sources-scala2.9.0.tgz
        cso-doc-scala2.9.0.tgz

as well as 

        cso.jar    -- the compiled CSO library
        cswing.jar -- the compiled CSwing library

The directory Attic/ contains earlier source distributions. I do not maintain
these distributions any longer.

Bernard Sufrin (Bernard.Sufrin@cs.ox.ac.uk)

DISCLAIMER: Much of this material was designed in 2008; and
doubtless the Scala ecosystem has changed since that time.
Nevertheless as I write in January 2012, the CSO library
still appears to function usefully (as witnessed by the two 
CSwing tests).

APOLOGY: I was distracted by a serious illness in my family
from incorporating Gavin Lowe's excellent reimplementation
of an extended-functionality Alt into this distribution. I
might get around to it before the 2011/2012 academic year is
out.

-------------------------------------------------------------------------
-------------------------------------------------------------------------
Compiling CSO [for Scala revision=2.9.0]

1.  Unpack cso-sources-scala-$(Revision).tgz in a new folder

1a. ~/Scala should be (or be a symbolic reference to) the scala distribution you plan
    to use. 
1b. ~/ScalaLocal should be a folder that contains the scalatasks.xml
    file included here.
 
2.  ant jar 

    Ignore warnings from the compiler; they probably reflect my lagging behind
    the scalaistas in removing deprecated features from my code.

If you want to see some (extreme) CSO programming then
CScala may be the place to look; otherwise look at my
published lecture notes for Concurrent Programming or those
of Gavin Lowe who succeeded me in teaching the course.

3. cd CScala
4. ant jar

The Lyfe and CSwingTest programs in CScala were designed as
a bandwidth-requirement test for an approach to programming
animations as collections of independently animated
processes communicating to a visualiser (and to each other)
using channels. I am convinced that the approach is useful.
Lyfe's sluggishness is not caused by the graphics (exercise:
what /is/ it caused by?), and CSwingTest is very fast, even
when there are thousands of independent objects being
anumated.

You can run them from ant. See ant -p for details. 

$Id: README.txt 516 2012-01-02 19:50:03Z sufrin $




