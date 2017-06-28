package se.henrikeriksson.greenhouse;

import com.pi4j.io.gpio.*;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.knowm.dropwizard.sundial.SundialBundle;
import org.knowm.dropwizard.sundial.SundialConfiguration;

import se.henrikeriksson.greenhouse.health.Health;
import se.henrikeriksson.greenhouse.resources.GpioPins;
import se.henrikeriksson.greenhouse.resources.GreenHouseStatus;
import javax.ws.rs.client.Client;

public class GreenHouseApplication extends Application<GreenHouseConfiguration> {

    public static void main(final String[] args) throws Exception {
        new GreenHouseApplication().run(args);
    }

    @Override
    public String getName() {
        return "GreenHouse";
    }
    GpioController gpio = null;
    GpioPinDigitalOutput wateringPin, acPin = null;
    GpioPinDigitalInput moisturePin = null;

    @Override
    public void initialize(final Bootstrap<GreenHouseConfiguration> bootstrap) {
        // TODO: application initialization
        // create gpio controller instance
        gpio = GpioFactory.getInstance();
        acPin = gpio.provisionDigitalOutputPin(GpioPins.getPinFromBOARD(40), "My fan", PinState.LOW);
        acPin.setShutdownOptions(true, PinState.LOW);
        wateringPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01,   // PIN
                "My LED",           // PIN FRIENDLY NAME (optional)
                PinState.LOW);      // PIN STARTUP STATE (optional)
        wateringPin.setShutdownOptions(true, PinState.LOW);

        moisturePin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, "My moisture sensor", PinPullResistance.PULL_DOWN);
        // set shutdown state for this input pin
        moisturePin.setShutdownOptions(true);



        bootstrap.addBundle(new SundialBundle<GreenHouseConfiguration>() {
            @Override
            public SundialConfiguration getSundialConfiguration(GreenHouseConfiguration configuration) {
                return configuration.getSundialConfiguration();
            }
        });
    }


    @Override
    public void run(final GreenHouseConfiguration configuration,
                    final Environment environment) {
        // TODO: implement application
        environment.healthChecks().register("myHealthCheck", new Health());

        //Now we added REST Client Resource named RESTClientController
        final Client client = new JerseyClientBuilder(environment).build("DemoRESTClient");

        final GreenHouseStatus statusResource = new GreenHouseStatus(configuration, wateringPin, acPin, moisturePin,client);
        environment.jersey().register(statusResource);

        // Add object to ServletContext for accessing from Sundial Jobs
        environment.getApplicationContext().setAttribute("led", wateringPin);
    }




}
