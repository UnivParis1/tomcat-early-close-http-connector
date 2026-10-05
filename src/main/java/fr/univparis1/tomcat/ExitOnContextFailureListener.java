package fr.univparis1.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;


public class ExitOnContextFailureListener implements LifecycleListener {

    @Override
    public void lifecycleEvent(LifecycleEvent event) {

        if (event.getSource() instanceof Context) {
            Context context = (Context) event.getSource();
            
            //System.err.println(context.getName() + " " + event.getType() + " " + context.getState());

            if (LifecycleState.FAILED.equals(context.getState())) {
                System.err.println("Webapp " + context.getName() + " failed, terminating JVM");
                System.exit(1);
            }
        } else {
            throw new RuntimeException("" + ExitOnContextFailureListener.class.getName() + "must be used in <Context>");
        }
    }
}
