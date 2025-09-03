You must implement a library for reliable broadcast communication among a set of faulty
processes, plus a simple application to test it (you are free to choose the application you
prefer to highlight the characteristics of the library).
The library must guarantee virtual synchrony, while ordering should be at least fifo.


Assumptions:
Assume (and leverage) a LAN scenario (i.e., link-layer broadcast is available).
You may also assume no processes fail during the time required for previous failures to be
recovered.