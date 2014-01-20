number_of_processes = 4
jar_name = test.jar
main_class = MainRunner


all:
	jar cvf bin/$(jar_name) -C build .

clean:
	@if test -f $(bin/$(jar_name)); then \
      rm bin/$(jar_name); \
      echo Removed file bin/$(jar_name); \
    else \
      echo Nothing to clean up, file does not exist.; \
    fi
 
up: clean all
	p2pmpirun -l bin/xferfiles -n 4 $(main_class)

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
