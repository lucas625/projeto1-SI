import com.mobilerobots.Aria.*;
import java.util.Scanner;
import java.util.Random;

public class destinyFinder {

    private static class Go extends ArAction {

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

        @Override
        public ArActionDesired fire(ArActionDesired currentDesired)
        {
            double speed;

            myDesired.reset();
            if ((robot.getPose().getX() >= objective.getX()-200 && robot.getPose().getX() <= objective.getX()+200) && (robot.getPose().getY() >= objective.getY()-200 && robot.getPose().getY() <= objective.getY()+200)) {

                myDesired.setVel(0);
                ArLog.log(ArLog.LogLevel.Terse,"At objective!");

            } else {

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

            }

            return myDesired;
        }

        public void setRobot(ArRobot robot)
        {
            setActionRobot(robot);
            mySonar = robot.findRangeDevice("sonar");
        }

    }

    private static class Turn extends ArAction {

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

        @Override
        public ArActionDesired fire(ArActionDesired currentDesired)
        {
            double left, right, front;

            myDesired.reset();

            double robotRadius = 300;

            double posX = objective.getX() - robot.getPose().getX();
            double posY = objective.getY() - robot.getPose().getY();
            double angObj = ArMath.atan2(posY, posX);
            if (angObj > 180) {
                angObj = angObj - 360;
            }
            double ang = angObj - robot.getPose().getTh();

            left = mySonar.currentReadingPolar(0, 100) - robotRadius;
            right = mySonar.currentReadingPolar(-100, 0) - robotRadius;
            front = mySonar.currentReadingPolar(0,0) - robotRadius;

            if (left > myThreshold && right > myThreshold && front > myThreshold) {
                myTurn = 0;
                myDesired.setDeltaHeading(ang);
            } else if (right > myThreshold && front > myThreshold) {
                myTurn = -1;
                myDesired.setDeltaHeading(myAmount*myTurn);
            } else if (left > myThreshold && front > myThreshold) {
                myTurn = 1;
                myDesired.setDeltaHeading(myAmount*myTurn);
            } else if (left > myThreshold && right > myThreshold) {
                /**/
                myTurn = 0;
                myDesired.setDeltaHeading(0);
            } else if (right > myThreshold) {
                myTurn = 0;
                myDesired.setDeltaHeading(0);
            } else if (left > myThreshold) {
                myTurn = 0;
                myDesired.setDeltaHeading(0);
            } else if (front > myThreshold) {
                myTurn = 0;

                if ((angObj + 3) >= robot.getPose().getTh() && (angObj - 3) <= robot.getPose().getTh()) {
                    do {
                        myTurn = new Random().nextInt(3) - 1;
                    } while (myTurn != 0);
                }
                myDesired.setDeltaHeading(myTurn*myAmount);
            } else {
                myTurn = -1;
                myDesired.setDeltaHeading(180);
            }

            ArLog.log(ArLog.LogLevel.Terse,"X: "+ robot.getPose().getX() + " Y: " + robot.getPose().getY() + " Th: " + robot.getPose().getTh() + " AngObj: " + angObj + " " + " Ang: " + ang + (front > myThreshold) + " " + (left > myThreshold) + " " + (right > myThreshold));

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

    static ArRobot robot = new ArRobot();
    static ArSonarDevice sonar = new ArSonarDevice();

    static ArPose objective;

    public static void main(String Args[]) {
        int xIn, yIn, xT, yT, lim;

        System.out.println("Digite a posição inicial do robô(x,y)");
        xIn = in.nextInt();
        yIn = in.nextInt();
        System.out.println("Digite o alvo(x,y)");
        xT = in.nextInt();
        yT = in.nextInt();
        System.out.println("Digite o ângulo inicial");
        lim = in.nextInt();
        objective = new ArPose(xT, yT, 0);

        Aria.init();

        ArSimpleConnector conn = new ArSimpleConnector(Args);

        // Create instances of the actions defined above, plus ArActionStallRecover,
        Go go = new Go(400, 50);
        Turn turn = new Turn(300, 15);

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
        robot.addAction(recover, 90);
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
