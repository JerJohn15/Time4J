/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2016 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (MachineTime.java) is part of project Time4J.
 *
 * Time4J is free software: You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Time4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Time4J. If not, see <http://www.gnu.org/licenses/>.
 * -----------------------------------------------------------------------
 */

package net.time4j.range;

import net.time4j.SI;
import net.time4j.base.UnixTime;
import net.time4j.engine.RealTime;
import net.time4j.engine.TimeMetric;
import net.time4j.engine.TimePoint;
import net.time4j.scale.TimeScale;
import net.time4j.scale.UniversalTime;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.time4j.scale.TimeScale.POSIX;
import static net.time4j.scale.TimeScale.UTC;


/**
 * <p>Represents a duration for machine times in decimal seconds with
 * nanosecond precision. </p>
 *
 * <p>Note: Other time units are NOT contained but can be used in construction
 * of a machine time. Example: </p>
 *
 * <pre>
 *  MachineTime&lt;TimeUnit&gt; mt = MachineTime.of(1, TimeUnit.HOURS);
 *  System.out.println(mt.contains(TimeUnit.HOURS)); // false
 *  System.out.println(mt.getSeconds); // 3600L
 * </pre>
 *
 * @param   <U> either {@code TimeUnit} or {@code SI}
 * @author  Meno Hochschild
 * @since   3.0
 * @see     TimeUnit#SECONDS
 * @see     TimeUnit#NANOSECONDS
 * @see     SI#SECONDS
 * @see     SI#NANOSECONDS
 * @doctags.concurrency {immutable}
 */
/*[deutsch]
 * <p>Repr&auml;sentiert eine Dauer f&uuml;r maschinelle Zeiten in dezimalen
 * Sekunden mit Nanosekundengenauigkeit. </p>
 *
 * <p>Hinweis: Andere Zeiteinheiten sind NICHT enthalten, k&ouml;nnen aber in
 * der Konstruktuion einer maschinellen Dauer verwendet werden. Beispiel: </p>
 *
 * <pre>
 *  MachineTime&lt;TimeUnit&gt; mt = MachineTime.of(1, TimeUnit.HOURS);
 *  System.out.println(mt.contains(TimeUnit.HOURS)); // false
 *  System.out.println(mt.getSeconds); // 3600L
 * </pre>
 *
 * @param   <U> either {@code TimeUnit} or {@code SI}
 * @author  Meno Hochschild
 * @since   3.0
 * @see     TimeUnit#SECONDS
 * @see     TimeUnit#NANOSECONDS
 * @see     SI#SECONDS
 * @see     SI#NANOSECONDS
 * @doctags.concurrency {immutable}
 */
public final class MachineTime<U>
    implements RealTime<U>, Comparable<MachineTime<U>>, Serializable {

    //~ Statische Felder/Initialisierungen --------------------------------

    private static final int MRD = 1000000000;

    private static final MachineTime<TimeUnit> POSIX_ZERO = new MachineTime<>(0, 0, POSIX);
    private static final MachineTime<SI> UTC_ZERO = new MachineTime<>(0, 0, UTC);

    /**
     * Metric on the POSIX scale (without leap seconds).
     *
     * @since   2.0
     */
    /*[deutsch]
     * Metrik auf der POSIX-Skala (ohne Schaltsekunden).
     *
     * @since   2.0
     */
    public static final TimeMetric<TimeUnit, MachineTime<TimeUnit>> ON_POSIX_SCALE = new Metric<>(POSIX);

    /**
     * <p>Metric on the UTC scale (inclusive leap seconds). </p>
     * 
     * <p>Time points before 1972 are not supported. </p>
     *
     * @since   2.0
     */
    /*[deutsch]
     * <p>Metrik auf der UTC-Skala (inklusive Schaltsekunden). </p>
     *
     * <p>Zeitpunkte vor 1972 werden nicht unterst&uuml;tzt. </p>
     *
     * @since   2.0
     */
    public static final TimeMetric<TimeUnit, MachineTime<SI>> ON_UTC_SCALE = new Metric<>(UTC);

    private static final long serialVersionUID = -4150291820807606229L;

    //~ Instanzvariablen --------------------------------------------------

    private transient final long seconds;
    private transient final int nanos;
    private transient final TimeScale scale;

    //~ Konstruktoren -----------------------------------------------------

    private MachineTime(
        long secs,
        int fraction,
        TimeScale scale
    ) {
        super();

        while (fraction < 0) {
            fraction += MRD;
            secs = Math.subtractExact(secs, 1);
        }

        while (fraction >= MRD) {
            fraction -= MRD;
            secs = Math.addExact(secs, 1);
        }

        if ((secs < 0) && (fraction > 0)) {
            secs++;
            fraction -= MRD;
        }

        this.seconds =  secs;
        this.nanos = fraction;
        this.scale = scale;

    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Creates a machine time duration on the POSIX scale. </p>
     *
     * @param   amount of units
     * @param   unit    helps to interprete given amount
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    /*[deutsch]
     * <p>Erzeugt eine Dauer als Maschinenzeit auf der POSIX-Skala. </p>
     *
     * @param   amount of units
     * @param   unit    helps to interprete given amount
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    public static MachineTime<TimeUnit> of(
        long amount,
        TimeUnit unit
    ) {

        if (unit.compareTo(TimeUnit.SECONDS) >= 0) {
            long secs =
                Math.multiplyExact(
                    amount,
                    TimeUnit.SECONDS.convert(1, unit));
            return ofPosixUnits(secs, 0);
        }

        long total =
            Math.multiplyExact(
                amount,
                TimeUnit.NANOSECONDS.convert(1, unit));
        long secs = Math.floorDiv(total, MRD);
        int fraction = (int) Math.floorMod(total, MRD);
        return ofPosixUnits(secs, fraction);

    }

    /**
     * <p>Creates a machine time duration on the UTC scale. </p>
     *
     * @param   amount of units
     * @param   unit    helps to interprete given amount
     * @return  new machine time duration
     * @since   2.0
     */
    /*[deutsch]
     * <p>Erzeugt eine Dauer als Maschinenzeit auf der UTC-Skala. </p>
     *
     * @param   amount of units
     * @param   unit    helps to interprete given amount
     * @return  new machine time duration
     * @since   2.0
     */
    public static MachineTime<SI> of(
        long amount,
        SI unit
    ) {

        switch (unit) {
            case SECONDS:
                return ofSIUnits(amount, 0);
            case NANOSECONDS:
                long secs = Math.floorDiv(amount, MRD);
                int fraction = (int) Math.floorMod(amount, MRD);
                return ofSIUnits(secs, fraction);
            default:
                throw new UnsupportedOperationException(unit.name());
        }

    }

    /**
     * <p>Creates a machine time duration on the POSIX scale. </p>
     *
     * @param   seconds     POSIX-seconds
     * @param   fraction    nanosecond part
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    /*[deutsch]
     * <p>Erzeugt eine Dauer als Maschinenzeit auf der POSIX-Skala. </p>
     *
     * @param   seconds     POSIX-seconds
     * @param   fraction    nanosecond part
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    public static MachineTime<TimeUnit> ofPosixUnits(
        long seconds,
        int fraction
    ) {

        if ((seconds == 0) && (fraction == 0)) {
            return POSIX_ZERO;
        }

        return new MachineTime<>(seconds, fraction, POSIX);

    }

    /**
     * <p>Creates a machine time duration on the UTC scale. </p>
     *
     * @param   seconds     SI-seconds
     * @param   fraction    nanosecond part
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    /*[deutsch]
     * <p>Erzeugt eine Dauer als Maschinenzeit auf der UTC-Skala. </p>
     *
     * @param   seconds     SI-seconds
     * @param   fraction    nanosecond part
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    public static MachineTime<SI> ofSIUnits(
        long seconds,
        int fraction
    ) {

        if ((seconds == 0) && (fraction == 0)) {
            return UTC_ZERO;
        }

        return new MachineTime<>(seconds, fraction, UTC);

    }

    /**
     * <p>Creates a machine time duration on the POSIX scale. </p>
     *
     * @param   seconds     decimal POSIX-seconds
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @throws  IllegalArgumentException if the argument is infinite or NaN
     * @since   2.0
     */
    /*[deutsch]
     * <p>Erzeugt eine Dauer als Maschinenzeit auf der POSIX-Skala. </p>
     *
     * @param   seconds     decimal POSIX-seconds
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @throws  IllegalArgumentException if the argument is infinite or NaN
     * @since   2.0
     */
    public static MachineTime<TimeUnit> ofPosixSeconds(double seconds) {

        if (Double.isInfinite(seconds) || Double.isNaN(seconds)) {
            throw new IllegalArgumentException("Invalid value: " + seconds);
        }

        long secs = (long) Math.floor(seconds);
        int fraction = (int) ((seconds - secs) * MRD);
        return ofPosixUnits(secs, fraction);

    }

    /**
     * <p>Creates a machine time duration on the POSIX scale. </p>
     *
     * @param   seconds     decimal POSIX-seconds
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    /*[deutsch]
     * <p>Erzeugt eine Dauer als Maschinenzeit auf der POSIX-Skala. </p>
     *
     * @param   seconds     decimal POSIX-seconds
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    public static MachineTime<TimeUnit> ofPosixSeconds(BigDecimal seconds) {

        BigDecimal secs = seconds.setScale(0, RoundingMode.FLOOR);
        int fraction =
            seconds.subtract(secs)
            .multiply(BigDecimal.valueOf(MRD))
            .setScale(0, RoundingMode.DOWN)
            .intValueExact();
        return ofPosixUnits(secs.longValueExact(), fraction);

    }

    /**
     * <p>Creates a machine time duration on the UTC scale. </p>
     *
     * @param   seconds     decimal SI-seconds
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @throws  IllegalArgumentException if the argument is infinite or NaN
     * @since   2.0
     */
    /*[deutsch]
     * <p>Erzeugt eine Dauer als Maschinenzeit auf der UTC-Skala. </p>
     *
     * @param   seconds     decimal SI-seconds
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @throws  IllegalArgumentException if the argument is infinite or NaN
     * @since   2.0
     */
    public static MachineTime<SI> ofSISeconds(double seconds) {

        if (Double.isInfinite(seconds) || Double.isNaN(seconds)) {
            throw new IllegalArgumentException("Invalid value: " + seconds);
        }

        long secs = (long) Math.floor(seconds);
        int fraction = (int) ((seconds - secs) * MRD);
        return ofSIUnits(secs, fraction);

    }

    /**
     * <p>Creates a machine time duration on the UTC scale. </p>
     *
     * @param   seconds     decimal SI-seconds
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    /*[deutsch]
     * <p>Erzeugt eine Dauer als Maschinenzeit auf der UTC-Skala. </p>
     *
     * @param   seconds     decimal SI-seconds
     * @return  new machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    public static MachineTime<SI> ofSISeconds(BigDecimal seconds) {

        BigDecimal secs = seconds.setScale(0, RoundingMode.FLOOR);
        int fraction =
            seconds.subtract(secs)
            .multiply(BigDecimal.valueOf(MRD))
            .setScale(0, RoundingMode.DOWN)
            .intValueExact();
        return ofSIUnits(secs.longValueExact(), fraction);

    }

    @Override
    public long getSeconds() {

        long secs = this.seconds;

        if (this.nanos < 0) {
            secs--;
        }

        return secs;

    }

    @Override
    public int getFraction() {

        int n = this.nanos;

        if (n < 0) {
            n += MRD;
        }

        return n;

    }

    /**
     * <p>Yields the related time scale. </p>
     *
     * @return  either {@code TimeScale.POSIX} or {@code TimeScale.UTC}
     * @since   2.0
     */
    /*[deutsch]
     * <p>Liefert die zugeh&ouml;rige Zeitskala. </p>
     *
     * @return  either {@code TimeScale.POSIX} or {@code TimeScale.UTC}
     * @since   2.0
     */
    public TimeScale getScale() {

        return this.scale;

    }

    @Override
    public List<Item<U>> getTotalLength() {

        List<Item<U>> tmp = new ArrayList<>(2);

        if (this.seconds != 0) {
            Object u = ((this.scale == UTC) ? SI.SECONDS : TimeUnit.SECONDS);
            U unit = cast(u);
            tmp.add(Item.of(Math.abs(this.seconds), unit));
        }

        if (this.nanos != 0) {
            Object u =
                ((this.scale == UTC) ? SI.NANOSECONDS : TimeUnit.NANOSECONDS);
            U unit = cast(u);
            tmp.add(Item.of(Math.abs(this.nanos), unit));
        }

        return Collections.unmodifiableList(tmp);

    }

    @Override
    public boolean contains(Object unit) {

        if (
            ((this.scale == POSIX) && TimeUnit.SECONDS.equals(unit))
            || ((this.scale == UTC) && SI.SECONDS.equals(unit))
        ) {
            return (this.seconds != 0);
        } else if (
            ((this.scale == POSIX) && TimeUnit.NANOSECONDS.equals(unit))
            || ((this.scale == UTC) && SI.NANOSECONDS.equals(unit))
        ) {
            return (this.nanos != 0);
        }

        return false;

    }

    @Override
    public long getPartialAmount(Object unit) {

        if (
            ((this.scale == POSIX) && TimeUnit.SECONDS.equals(unit))
            || ((this.scale == UTC) && SI.SECONDS.equals(unit))
        ) {
            return Math.abs(this.seconds);
        } else if (
            ((this.scale == POSIX) && TimeUnit.NANOSECONDS.equals(unit))
            || ((this.scale == UTC) && SI.NANOSECONDS.equals(unit))
        ) {
            return Math.abs(this.nanos);
        }

        return 0;

    }

    @Override
    public boolean isNegative() {

        return ((this.seconds < 0) || (this.nanos < 0));

    }

    @Override
    public boolean isPositive() {

        return ((this.seconds > 0) || (this.nanos > 0));

    }

    @Override
    public boolean isEmpty() {

        return (this.seconds == 0) && (this.nanos == 0);

    }

    /**
     * <p>Add given temporal amount to this machine time. </p>
     *
     * @param   amount  the amount to be added
     * @param   unit    the related time unit
     * @return  result of addition
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    /*[deutsch]
     * <p>Addiert den angegebenen Zeitbetrag zu dieser maschinellen Dauer. </p>
     *
     * @param   amount  the amount to be added
     * @param   unit    the related time unit
     * @return  result of addition
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    public MachineTime<U> plus(
        long amount,
        U unit
    ) {

        long s = this.seconds;
        int f = this.nanos;

        if (this.scale == POSIX) {
            TimeUnit u = TimeUnit.class.cast(unit);

            if (u.compareTo(TimeUnit.SECONDS) >= 0) {
                s =
                    Math.addExact(
                        s,
                        Math.multiplyExact(
                            amount,
                            TimeUnit.SECONDS.convert(1, u))
                    );
            } else {
                long total =
                    Math.addExact(
                        f,
                        Math.multiplyExact(
                            amount,
                            TimeUnit.NANOSECONDS.convert(1, u))
                    );
                s = Math.addExact(s, Math.floorDiv(total, MRD));
                f = (int) Math.floorMod(total, MRD);
            }
        } else {
            switch (SI.class.cast(unit)) {
                case SECONDS:
                    s = Math.addExact(s, amount);
                    break;
                case NANOSECONDS:
                    long total = Math.addExact(f, amount);
                    s = Math.addExact(s, Math.floorDiv(total, MRD));
                    f = (int) Math.floorMod(total, MRD);
                    break;
                default:
                    throw new UnsupportedOperationException(unit.toString());
            }
        }

        return new MachineTime<>(s, f, this.scale);

    }

    /**
     * <p>Add given temporal amount to this machine time. </p>
     *
     * @param   duration    other machine time to be added
     * @return  result of addition
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    /*[deutsch]
     * <p>Addiert den angegebenen Zeitbetrag zu dieser maschinellen Dauer. </p>
     *
     * @param   duration    other machine time to be added
     * @return  result of addition
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    public MachineTime<U> plus(MachineTime<U> duration) {

        if (duration.isEmpty()) {
            return this;
        } else if (this.isEmpty()) {
            return duration;
        }

        long s = Math.addExact(this.seconds, duration.seconds);
        int f = this.nanos + duration.nanos;
        return new MachineTime<>(s, f, this.scale);

    }

    /**
     * <p>Subtracts given temporal amount from this machine time. </p>
     *
     * @param   amount  the amount to be subtracted
     * @param   unit    the related time unit
     * @return  difference result
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    /*[deutsch]
     * <p>Subtrahiert den angegebenen Zeitbetrag von dieser maschinellen
     * Dauer. </p>
     *
     * @param   amount  the amount to be subtracted
     * @param   unit    the related time unit
     * @return  difference result
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    public MachineTime<U> minus(
        long amount,
        U unit
    ) {

        return this.plus(Math.negateExact(amount), unit);

    }

    /**
     * <p>Subtracts given temporal amount from this machine time. </p>
     *
     * @param   duration    other machine time to be subtracted
     * @return  difference result
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    /*[deutsch]
     * <p>Subtrahiert den angegebenen Zeitbetrag von dieser maschinellen
     * Dauer. </p>
     *
     * @param   duration    other machine time to be subtracted
     * @return  difference result
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    public MachineTime<U> minus(MachineTime<U> duration) {

        if (duration.isEmpty()) {
            return this;
        } else if (this.isEmpty()) {
            return duration.inverse();
        }

        long s = Math.subtractExact(this.seconds, duration.seconds);
        int f = this.nanos - duration.nanos;
        return new MachineTime<>(s, f, this.scale);

    }

    /**
     * <p>Converts this machine duration to its absolute amount. </p>
     *
     * @return  absolute machine time duration, always non-negative
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    /*[deutsch]
     * <p>Wandelt eine maschinelle Dauer in ihren Absolutbetrag um. </p>
     *
     * @return  absolute machine time duration, always non-negative
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    public MachineTime<U> abs() {

        if (this.isNegative()) {
            return new MachineTime<>(
                Math.negateExact(this.seconds), -this.nanos, this.scale);
        } else {
            return this;
        }

    }

    /**
     * <p>Creates a copy with inversed sign. </p>
     *
     * @return  negated machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    /*[deutsch]
     * <p>Wandelt eine maschinelle Dauer in ihr negatives &Auml;quivalent
     * um. </p>
     *
     * @return  negated machine time duration
     * @throws  ArithmeticException in case of numerical overflow
     * @since   2.0
     */
    public MachineTime<U> inverse() {

        if (this.isEmpty()) {
            return this;
        }

        return new MachineTime<>(
            Math.negateExact(this.seconds), -this.nanos, this.scale);

    }

    /**
     * <p>Multiplies this duration with given factor. </p>
     *
     * @param   factor  multiplicand
     * @return  changed copy of this duration
     */
    /*[deutsch]
     * <p>Multipliziert diese Dauer mit dem angegebenen Faktor. </p>
     *
     * @param   factor  multiplicand
     * @return  changed copy of this duration
     */
    public MachineTime<U> multipliedBy(long factor) {

        if (factor == 1) {
            return this;
        }

        BigDecimal value =
            this.toBigDecimal().multiply(BigDecimal.valueOf(factor));
        MachineTime<?> mt;

        if (this.scale == POSIX) {
            mt = MachineTime.ofPosixSeconds(value);
        } else {
            mt = MachineTime.ofSISeconds(value);
        }

        return cast(mt);

    }

    /**
     * <p>Divides this duration by given divisor using rounding
     * mode {@code HALF_UP}. </p>
     *
     * @param   divisor     divisor
     * @return  changed copy of this duration
     * @see     RoundingMode#HALF_UP
     * @deprecated Use {@link #dividedBy(long, RoundingMode) dividedBy(long, RoundingMode.HALF_UP} instead
     */
    /*[deutsch]
     * <p>Dividiert diese Dauer durch den angegebenen Teiler und
     * benutzt die kaufm&auml;nnische Rundung. </p>
     *
     * @param   divisor     Teiler
     * @return  ge&auml;nderte Kopie dieser Dauer
     * @see     RoundingMode#HALF_UP
     * @deprecated Use {@link #dividedBy(long, RoundingMode) dividedBy(long, RoundingMode.HALF_UP} instead
     */
    @Deprecated
    public MachineTime<U> dividedBy(long divisor) {

        return this.dividedBy(divisor, RoundingMode.HALF_UP);

    }

    /**
     * <p>Divides this duration by given divisor using given rounding mode. </p>
     *
     * @param   divisor         divisor
     * @param   roundingMode    rounding mode to be used in division
     * @return  changed copy of this duration
     * @since   3.23/4.19
     */
    /*[deutsch]
     * <p>Dividiert diese Dauer durch den angegebenen Teiler und benutzt die angegebene Rundung. </p>
     *
     * @param   divisor         divisor
     * @param   roundingMode    rounding mode to be used in division
     * @return  changed copy of this duration
     * @since   3.23/4.19
     */
    public MachineTime<U> dividedBy(
        long divisor,
        RoundingMode roundingMode
    ) {

        if (divisor == 1) {
            return this;
        }

        BigDecimal value =
            this.toBigDecimal().setScale(9, RoundingMode.FLOOR).divide(new BigDecimal(divisor), roundingMode);
        MachineTime<?> mt;

        if (this.scale == POSIX) {
            mt = MachineTime.ofPosixSeconds(value);
        } else {
            mt = MachineTime.ofSISeconds(value);
        }

        return cast(mt);

    }

    @Override
    public <T extends TimePoint<? super U, T>> T addTo(T time) {

        U s, f;

        if (this.scale == POSIX) {
            s = cast(TimeUnit.SECONDS);
            f = cast(TimeUnit.NANOSECONDS);
        } else {
            s = cast(SI.SECONDS);
            f = cast(SI.NANOSECONDS);
        }

        return time.plus(this.seconds, s).plus(this.nanos, f);

    }

    @Override
    public <T extends TimePoint<? super U, T>> T subtractFrom(T time) {

        U s, f;

        if (this.scale == POSIX) {
            s = cast(TimeUnit.SECONDS);
            f = cast(TimeUnit.NANOSECONDS);
        } else {
            s = cast(SI.SECONDS);
            f = cast(SI.NANOSECONDS);
        }

        return time.minus(this.seconds, s).minus(this.nanos, f);

    }

    /**
     * <p>Compares the absolute lengths and is equivalent to {@code abs().compareTo(other.abs()) < 0}. </p>
     *
     * @param   other   another machine time to be compared with
     * @return  boolean
     * @see     #compareTo(MachineTime)
     * @see     #isLongerThan(MachineTime)
     * @since   3.20/4.16
     */
    /*[deutsch]
     * <p>Vergleicht die absoluten L&auml;ngen und ist &auml;quivalent zu {@code abs().compareTo(other.abs()) < 0}. </p>
     *
     * @param   other   another machine time to be compared with
     * @return  boolean
     * @see     #compareTo(MachineTime)
     * @see     #isLongerThan(MachineTime)
     * @since   3.20/4.16
     */
    public boolean isShorterThan(MachineTime<U> other) {

        return (this.abs().compareTo(other.abs()) < 0);

    }

    /**
     * <p>Compares the absolute lengths and is equivalent to {@code abs().compareTo(other.abs()) > 0}. </p>
     *
     * @param   other   another machine time to be compared with
     * @return  boolean
     * @see     #compareTo(MachineTime)
     * @see     #isShorterThan(MachineTime)
     * @since   3.20/4.16
     */
    /*[deutsch]
     * <p>Vergleicht die absoluten L&auml;ngen und ist &auml;quivalent zu {@code abs().compareTo(other.abs()) > 0}. </p>
     *
     * @param   other   another machine time to be compared with
     * @return  boolean
     * @see     #compareTo(MachineTime)
     * @see     #isShorterThan(MachineTime)
     * @since   3.20/4.16
     */
    public boolean isLongerThan(MachineTime<U> other) {

        return (this.abs().compareTo(other.abs()) > 0);

    }

    /**
     * <p>Method of the {@code Comparable}-interface. </p>
     *
     * @param   other   another machine time to be compared with
     * @return  negative, zero or positive integer if this instance is shorter, equal or longer than other one
     * @throws  ClassCastException if this and the other machine time have different time scales
     * @see     #isShorterThan(MachineTime)
     * @see     #isLongerThan(MachineTime)
     * @since   3.20/4.16
     */
    /*[deutsch]
     * <p>Methode des {@code Comparable}-Interface. </p>
     *
     * @param   other   another machine time to be compared with
     * @return  negative, zero or positive integer if this instance is shorter, equal or longer than other one
     * @throws  ClassCastException if this and the other machine time have different time scales
     * @see     #isShorterThan(MachineTime)
     * @see     #isLongerThan(MachineTime)
     * @since   3.20/4.16
     */
    @Override
    public int compareTo(MachineTime<U> other) {

        if (this.scale == other.scale) {
            if (this.seconds < other.seconds) {
                return -1;
            } else if (this.seconds > other.seconds) {
                return 1;
            } else {
                return (this.nanos - other.nanos);
            }
        } else {
            throw new ClassCastException("Different time scales.");
        }

    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj instanceof MachineTime) {
            MachineTime<?> that = (MachineTime<?>) obj;
            return (
                (this.seconds == that.seconds)
                && (this.nanos == that.nanos)
                && (this.scale == that.scale));
        } else {
            return false;
        }

    }

    @Override
    public int hashCode() {

        int hash = 7;
        hash = 23 * hash + (int) (this.seconds ^ (this.seconds >>> 32));
        hash = 23 * hash + this.nanos;
        hash = 23 * hash + this.scale.hashCode();
        return hash;

    }

    /**
     * <p>Returns a format in technical notation including the name of the underlying time scale. </p>
     *
     * @return  String like &quot;-5s [POSIX]&quot; or &quot;4.123456789s [UTC]&quot;
     */
    /*[deutsch]
     * <p>Returns a format in technical notation including the name of the underlying time scale. </p>
     *
     * @return  String like &quot;-5s [POSIX]&quot; or &quot;4.123456789s [UTC]&quot;
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        this.createNumber(sb);
        sb.append("s [");
        sb.append(this.scale.name());
        sb.append(']');
        return sb.toString();

    }

    /**
     * <p>Converts this machine time duration into a decimal number of seconds. </p>
     *
     * @return  BigDecimal
     */
    /*[deutsch]
     * <p>Wandelt diese maschinelle Dauer in einen dezimalen Sekundenbetrag um. </p>
     *
     * @return  BigDecimal
     */
    public BigDecimal toBigDecimal() {

        StringBuilder sb = new StringBuilder();
        this.createNumber(sb);
        return new BigDecimal(sb.toString());

    }

    private void createNumber(StringBuilder sb) {

        if (this.isNegative()) {
            sb.append('-');
            sb.append(Math.abs(this.seconds));
        } else {
            sb.append(this.seconds);
        }

        if (this.nanos != 0) {
            sb.append('.');
            String fraction = String.valueOf(Math.abs(this.nanos));
            for (int i = 9 - fraction.length(); i > 0; i--) {
                sb.append('0');
            }
            sb.append(fraction);
        }

    }

    @SuppressWarnings("unchecked")
    private static <U> U cast(Object unit) {

        return (U) unit;

    }

    /**
     * @serialData  Uses <a href="../../../serialized-form.html#net.time4j.range.SPX">
     *              a dedicated serialization form</a> as proxy. The layout
     *              is bit-compressed. The first byte contains within the
     *              six most significant bits the type id {@code 7} and as
     *              least significant bit the value 1 if this instance uses
     *              the UTC-scale. Then the bytes for the seconds and fraction
     *              follow. The fraction bytes are only written if the fraction
     *              is not zero. In that case, the second least significant bit
     *              of the header is set, too.
     *
     * Schematic algorithm:
     *
     * <pre>
     *      byte header = (7 &lt;&lt; 2);
     *      if (scale == TimeScale.UTC) header |= 1;
     *      if (this.getFraction() &gt; 0) header |= 2;
     *      out.writeByte(header);
     *      out.writeLong(getSeconds());
     *      if (this.getFraction() &gt; 0) {
     *          out.writeInt(getFraction());
     *      }
     * </pre>
     *
     * @return  replacement object in serialization graph
     */
    private Object writeReplace() {

        return new SPX(this, SPX.MACHINE_TIME_TYPE);

    }

    /**
     * @serialData  Blocks because a serialization proxy is required.
     * @param       in      object input stream
     * @throws      InvalidObjectException (always)
     */
    private void readObject(ObjectInputStream in)
        throws IOException {

        throw new InvalidObjectException("Serialization proxy required.");

    }

    //~ Innere Klassen ----------------------------------------------------

    private static class Metric<U>
        implements TimeMetric<TimeUnit, MachineTime<U>> {

        //~ Instanzvariablen ----------------------------------------------

        private final TimeScale scale;

        //~ Konstruktoren -------------------------------------------------

        private Metric(TimeScale scale) {
            super();

            this.scale = scale;

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public
        <T extends TimePoint<? super TimeUnit, T>> MachineTime<U> between(
            T start,
            T end
        ) {

            long secs;
            int nanos;

            if (
                (this.scale == UTC)
                && (start instanceof UniversalTime)
            ) {
                UniversalTime t1 = (UniversalTime) start;
                UniversalTime t2 = (UniversalTime) end;
                long utc2 = t2.getElapsedTime(UTC);
                long utc1 = t1.getElapsedTime(UTC);
                if (utc2 < 0 || utc1 < 0) {
                    throw new UnsupportedOperationException(
                        "Cannot calculate SI-duration before 1972-01-01.");
                }
                secs = utc2 - utc1;
                nanos = t2.getNanosecond(UTC) - t1.getNanosecond(UTC);
            } else if (start instanceof UnixTime) {
                UnixTime t1 = (UnixTime) start;
                UnixTime t2 = (UnixTime) end;
                secs = t2.getPosixTime() - t1.getPosixTime();
                nanos = t2.getNanosecond() - t1.getNanosecond();
            } else {
                throw new UnsupportedOperationException(
                    "Machine time requires objects of type 'UnixTime'.");
            }

            return new MachineTime<>(secs, nanos, this.scale);

        }

    }

}
