rootdir = $(realpath .)
log_output_file = out.txt
number_of_processes = 4
jar_name = gossip_simulation.jar
main_class = ch.bfh.ti.ostes2.gossip.MainRunner

# Default target - show help, don't actually DO something!
help:
	@echo ""
	@echo "Makefile to build and run the Gossip-application (based on P2P-MPI  )."
	@echo "( This application requires P2P-MPI >= 0.24.0 and JDK >= 1.6        )"
	@echo "( http://grid.u-strasbg.fr/p2pmpi/download/                         )"
	@echo "( http://www.oracle.com/technetwork/java/index.html                 )"
	@echo ""
	@echo "The following make targets are available:"
	@echo "  * build       	- Build a JAR-file out of the compiled(!) class and manifest(!) files."
	@echo "  * clean       	- Remove eventual JAR-files."
	@echo "  * up     		- Clean and run the application."
	@echo "  * oup                 - Clean and run the application. Outputs get outputted to ./$(log_output_file)."
	@echo ""
	@echo "  * mpiboot      	- Runs the super node and boots mpi."
	@echo "  * mpihalt 		- Stops mpi and the super node."
	@echo "  * mpirestart		- Restarts mpi."
	@echo ""
	@echo "  * xfer                - Creates the xferfile based on"
	@echo "                          pwd and jar_name."
	@echo ""
	@echo "Typical usage:"
	@echo "  * make mpihalt (just to assure a clean env.)"
	@echo "  * make mpiboot"
	@echo "  * make up"
	@echo ""

jar:
	jar cvf bin/$(jar_name) -C build .
	jar uvf bin/$(jar_name) config.properties

clean:
	@if test -f $(bin/$(jar_name)); then \
      rm bin/$(jar_name); \
      echo Removed file bin/$(jar_name); \
    else \
      echo Nothing to clean up, file does not exist.; \
    fi

xfer:
	@echo $(rootdir)/bin/$(jar_name) > $(rootdir)/bin/xferfiles

up: clean jar xfer
	p2pmpirun -l bin/xferfiles -n $(number_of_processes) $(main_class)

oup: up
	p2pmpirun -l bin/xferfiles -n $(number_of_processes) $(main_class) > $(log_output_file) 2>&1 
mpiboot:
	runSuperNode
	sleep 1
	mpiboot

mpihalt:
	mpihalt
	sleep 1
	stopSuperNode

mpirestart:
	mpihalt
	sleep 2
	mpiboot
