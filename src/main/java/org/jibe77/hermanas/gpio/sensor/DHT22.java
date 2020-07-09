package org.jibe77.hermanas.gpio.sensor;

import org.jibe77.hermanas.gpio.GpioHermanasController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Implements the DHT22 / AM2302 reading in Java using Pi4J.
 *
 * See sensor specification sheet for details.
 *
 * @author Doug Culnane
 */
@Component
public class DHT22 {

    GpioHermanasController gpioHermanasController;

    /**
     * Time in nanoseconds to separate ZERO and ONE signals.
     */
    public static final int LONGEST_ZERO = 50000;

    /**
     * Minimum time in milliseconds to wait between reads of sensor.
     */
    public static final int MIN_MILLISECS_BETWEEN_READS = 2500;

    /**
     * PI4J Pin number.
     */
    @Value("${sensor.gpio.address}")
    private int pinNumber;

    @Value("${sensor.gpio.timeout.in.ms}")
    private int timeoutInMilliseconds;

    /**
     * 40 bit Data from sensor
     */
    private byte[] data = null;

    /**
     * Value of last successful humidity reading.
     */
    private Double humidity = null;

    /**
     * Value of last successful temperature reading.
     */
    private Double temperature = null;

    /**
     * Last read attempt
     */
    private Long lastRead = null;

    public DHT22(GpioHermanasController gpioHermanasController) {
        this.gpioHermanasController = gpioHermanasController;
    }

    /**
     * Communicate with sensor to get new reading data.
     * @throws ExecutionException
     * @throws InterruptedException
     *
     * @throws Exception if failed to successfully read data.
     */
    private void getData() throws IOException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ReadSensorFuture readSensor = new ReadSensorFuture();
        Future<byte[]> future = executor.submit(readSensor);
        // Reset data
        data = new byte[5];
        try {
            data = future.get(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
            readSensor.close();
        } catch (Exception e) {
            readSensor.close();
            future.cancel(true);
            executor.shutdown();
            throw new IOException(e);
        }
        readSensor.close();
        executor.shutdown();
    }

    public boolean doReadLoop() throws InterruptedException, IOException {
        Hashtable<IOException, Integer> exceptions = new Hashtable<IOException, Integer>();
        for (int i=0; i < 10; i++) {
            try {
                if (read(true)) {
                    return true;
                }
            } catch (IOException e) {
                if (Objects.isNull(exceptions.get(e))) {
                    exceptions.put(e, 1);
                } else {
                    exceptions.put(e, exceptions.get(e).intValue() + 1);
                }
            }
            Thread.sleep(DHT22.MIN_MILLISECS_BETWEEN_READS);
        }
        // return the most common exception.
        IOException returnException = null;
        int exceptionCount =  0;
        for (IOException e : exceptions.keySet()) {
            if (exceptions.get(e).intValue() > exceptionCount) {
                returnException = e;
            }
        }
        throw returnException;
    }

    /**
     * Make one new sensor reading.
     *
     * @return
     * @throws Exception
     */
    public boolean read() throws Exception {
        return read(true);
    }

    /**
     * Make a new sensor reading
     *
     * @param checkParity Should a parity check be performed?
     * @return
     * @throws ValueOutOfOperatingRangeException
     * @throws ParityCheckException
     * @throws IOException
     */
    public boolean read(boolean checkParity) throws ValueOutOfOperatingRangeException, ParityCheckException, IOException {
        checkLastReadDelay();
        lastRead = System.currentTimeMillis();
        getData();
        if (checkParity) {
            checkParity();
        }

        // Operating Ranges from specification sheet.
        // humidity 0-100
        // temperature -40~80
        double newHumidityValue = getReadingValueFromBytes(data[0], data[1]);
        if (newHumidityValue >= 0 && newHumidityValue <= 100) {
            humidity = newHumidityValue;
        } else {
            throw new ValueOutOfOperatingRangeException();
        }
        double newTemperatureValue = getReadingValueFromBytes(data[2], data[3]);
        if (newTemperatureValue >= -40 && newTemperatureValue < 85) {
            temperature = newTemperatureValue;
        } else {
            throw new ValueOutOfOperatingRangeException();
        }
        lastRead = System.currentTimeMillis();
        return true;
    }

    private void checkLastReadDelay() throws IOException {
        if (Objects.nonNull(lastRead)) {
            if (lastRead > System.currentTimeMillis() - 2000) {
                throw new IOException("Last read was under 2 seconds ago. Please wait longer between reads!");
            }
        }
    }

    protected static double getReadingValueFromBytes(final byte hi, final byte low) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(hi);
        bb.put(low);
        short shortVal = bb.getShort(0);
        Double doubleValue = new Double(shortVal) / 10;

        // When highest bit of temperature is 1, it means the temperature is below 0 degree Celsius.
        if (1 == ((hi >> 7) & 1)) {
            doubleValue = (doubleValue + 3276.8) * -1d;
        }

        return doubleValue;
    }

    private void checkParity() throws ParityCheckException {
        if (!(data[4] == (data[0] + data[1] + data[2] + data[3]))) {
            throw new ParityCheckException();
        }
    }

    public Double getHumidity() {
        return humidity;
    }

    public Double getTemperature() {
        return temperature;
    }

    /**
     * Callable Future for reading sensor.  Allows timeout if it gets stuck.
     */
    private class ReadSensorFuture implements Callable<byte[]>, Closeable {

        private boolean keepRunning = true;

        public ReadSensorFuture() {
            gpioHermanasController.initSensor(pinNumber);
        }

        @Override
        public byte[] call() throws Exception {

            // do expensive (slow) stuff before we start and privoritize thread.
            long startTime = System.nanoTime();
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            sendStartSignal();
            waitForResponseSignal();
            byte[] data = gpioHermanasController.fetchData(pinNumber, keepRunning, startTime);

            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            return data;
        }

        private void sendStartSignal() {
            gpioHermanasController.sendStartSignal(pinNumber);
        }

        /**
         * AM2302 will pull low 80us as response signal, then
         * AM2302 pulls up 80us for preparation to send data.
         */
        private void waitForResponseSignal() {
            gpioHermanasController.waitForResponseSignal(pinNumber, keepRunning);
        }

        @Override
        public void close() throws IOException {
            keepRunning = false;
            gpioHermanasController.close(pinNumber);
        }
    }

    public class ParityCheckException extends IOException {
        private static final long serialVersionUID = 1L;
    }

    public class ValueOutOfOperatingRangeException extends IOException {
        private static final long serialVersionUID = 1L;
    }

}