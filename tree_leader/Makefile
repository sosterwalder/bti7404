all:
	jar cvf TreeLeader.jar *.class

clean:
	@if test -f $(TreeLeader.jar); then \
      rm TreeLeader.jar; \
      echo Removed file TreeLeader.jar; \
    else \
      echo Nothing to clean up, file does not exist.; \
    fi
 
up: clean all
	p2pmpirun -l xferfiles -n 10 TreeLeader