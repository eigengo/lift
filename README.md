lift
====
A demonstration of an application that uses _wearable devices_ (Pebble)—in combination with a mobile app—to submit 
physical (i.e. accelerometer, compass) and biological (i.e. heart rate) information to a CQRS/ES cluster to be
analysed.

The result is automatically constructed exercise log, which includes:

* the kind of exercise (biceps curl, shoulder press, and other such tortures)
* the intensity (light, moderate, hard)—nota bene that intensity != weight

---

It is also an excellent demonstration of large reactive application. It is _event-driven_: throughout the application,
it uses message-passing to provide loose-coupling and asynchrony between components. It is _elastic_: the users and 
their exercises—the domain—is sharded across the cluster. It is _resilient_: its components can recover at the 
appropriate level, be it single actor, trees of actors or entire JVMs. It also uses event sourcing to ensure that 
even catastrophic failures and the inevitable bugs can be recovered from. It is _responsive_: it does not block, and 
it is capable of distributing the load across the cluster. 

It combines near-real-time machine classification needed for immediate exercise feedback, with "offline" model 
upgrades. Once upgraded, the event-sourced nature of the system allows us to re-apply the new model to the old
data, and thus provide the users with better data.
