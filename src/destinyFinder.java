import com.mobilerobots.Aria.*;
import java.util.Scanner;

public class destinyFinder {

    private class Go extends ArAction {

        private double mySpeed, myDistance;
        private ArActionDesired myDesired;
        private ArRangeDevice mySonar;

        public Go(double speed, double distance)
        {
            super("Go");
            mySpeed = speed;
            myDistance = distance;
            myDesired = new ArActionDesired();
        }

        public ArActionDesired fire(ArActionDesired currentDesired)
        {
            double speed;

            myDesired.reset();

            double robotRadius = 100;
            double range = mySonar.currentReadingPolar(-70, 70) - robotRadius;

            if (range > myDistance) {
                speed = range * .45;
                if (speed > mySpeed)
                    speed = mySpeed;
                myDesired.setVel(speed);
            } else {
                myDesired.setVel(0);
            }
            return myDesired;
        }

        public void setRobot(ArRobot robot)
        {
            setActionRobot(robot);
            mySonar = robot.findRangeDevice("sonar");
        }

    }

    private class Turn extends ArAction {

        private double myAmount, myThreshold;
        private int myTurn;
        private ArActionDesired myDesired;
        private ArRangeDevice mySonar;

        public Turn(double threshold, double amount)
        {
            super("Turn");
            myAmount = amount;
            myThreshold = threshold;
            myTurn = 0;
            myDesired = new ArActionDesired();
        }

        public ArActionDesired fire(ArActionDesired currentDesired)
        {
            double left, right;

            myDesired.reset();

            double robotRadius = 100;
            left = mySonar.currentReadingPolar(0, 100) - robotRadius;
            right = mySonar.currentReadingPolar(-100, 0) - robotRadius;

            if (left > myThreshold && right > myThreshold) {
                myTurn = 0;
                myDesired.setDeltaHeading(0);
            } else if (myTurn != 0) {
                myDesired.setDeltaHeading(myAmount * myTurn);
            } else if (left < right) {
                myTurn = -1;
                myDesired.setDeltaHeading(myAmount * myTurn);
            } else {
                myTurn = 1;
                myDesired.setDeltaHeading(myAmount * myTurn);
            }

            return myDesired;
        }

        public void setRobot(ArRobot robot)
        {
            setActionRobot(robot);
            mySonar = robot.findRangeDevice("sonar");
        }

    }

    static {
        try {
            System.loadLibrary("AriaJava");
        } catch(UnsatisfiedLinkError e) {
            System.err.println("Native code library libAriaJava failed to load. Make sure that its directory is in your library path. See javaExamples/README.txt and the chapter on Dynamic Linking Problems in the SWIG Java Documentation (http://www.swig.org) for help.\n" + e);
            System.exit(1);
        }
    }

    static Scanner in = new Scanner(System.in);

    public static void main(String Args[]) {
        String auxiliary;

        System.out.println("Digite X e Y do robô, no formato 'X Y':");
         auxiliary

        Aria.init();

        ArSimpleConnector conn = new ArSimpleConnector(Args);
        ArRobot robot = new ArRobot();
        ArSonarDevice sonar = new ArSonarDevice();

        // Create instances of the actions defined above, plus ArActionStallRecover,
        ExampleGoAction go = new ExampleGoAction(500, 350);
        ExampleTurnAction turn = new ExampleTurnAction(400, 10);

        // a predefined action from Aria.
        ArActionStallRecover recover = new ArActionStallRecover();


        // Parse all command-line arguments
        if(!Aria.parseArgs())
        {
            Aria.logOptions();
            System.exit(1);
        }

        // Connect to the robot
        if(!conn.connectRobot(robot))
        {
            ArLog.log(ArLog.LogLevel.Terse, "actionExample: Could not connect to robot! Exiting.");
            System.exit(2);
        }

        // Add the range device to the robot. You should add all the range
        // devices and such before you add actions
        robot.addRangeDevice(sonar);


        // Add our actions in order. The second argument is the priority,
        // with higher priority actions going first, and possibly pre-empting lower
        // priority actions.
        robot.addAction(recover, 60);
        robot.addAction(turn, 50);
        robot.addAction(go, 40);

        // Enable the motors, disable amigobot sounds
        robot.enableMotors();

        // Run the robot processing cycle.
        // 'true' means to return if it loses connection,
        // after which we exit the program.
        robot.run(true);

        Aria.exit(0);
    }

}
