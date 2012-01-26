package ox.cso;

// Two watchdogs, to test whether traces satisfy properties.

/**
 A WatchDog  takes a trace specification <code>spec</code>,
 i.e. a function from sequences of events to Boolean. The
 specification is checked whenever a new event is logged.
 A <code>WatchDogException</code> (embedding the trace) is thrown if the
 specification is not satisfied.
*/
class WatchDog[E](spec : Seq[E] => Boolean){

  private var tr = new scala.collection.mutable.ListBuffer[E]; // the trace of events so far

  /** Log an event and re-evaluate <code>spec</code> */
  def log(e:E) = synchronized {
    tr += e;
    if (!spec(tr)) throw new WatchDogException(tr)
  }
}

/** 
    A stateful watchdog takes a predicate <code>ok</code> on states
    and a state transition function <code>update</code>.
    It maintains a trace of events and a state (initially <code>initS</code>) 
    that is updated for each logged event, after which
    <code>ok</code> tests whether the new state is
    acceptable.  A <code>StatefulWatchDogException</code> (embedding
    the state and the trace) is thrown if the state is not
    acceptable.
*/
class StatefulWatchDog[S,E](initS : S, update : (S,E) => S, ok : S => Boolean){
 
  private var state = initS; // the current state
  private var tr = new scala.collection.mutable.ListBuffer[E]; 
    // the trace of events so far
  
  /** Log an event and re-evaluate <code>ok</code> */
  def log(e:E) = synchronized{
    state = update(state,e); 
    tr += e;
    if (!ok(state)) throw new StatefulWatchDogException(state, tr)
  }
}
 
case class WatchDogException[E](_trace: Seq[E]) extends RuntimeException("Illegal Trace")
{ val trace = _trace
} 

case class StatefulWatchDogException[S,E](_state: S, _trace: Seq[E]) 
      extends RuntimeException("Illegal State")
{ val trace = _trace
  val state = _state
} 



