package net.sf.openrocket.optimization.rocketoptimization.parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.openrocket.aerodynamics.AerodynamicCalculator;
import net.sf.openrocket.aerodynamics.BarrowmanCalculator;
import net.sf.openrocket.aerodynamics.FlightConditions;
import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.l10n.Translator;
import net.sf.openrocket.masscalc.MassCalculator;
import net.sf.openrocket.optimization.general.OptimizationException;
import net.sf.openrocket.optimization.rocketoptimization.OptimizableParameter;
import net.sf.openrocket.rocketcomponent.FlightConfiguration;
import net.sf.openrocket.rocketcomponent.RocketComponent;
import net.sf.openrocket.rocketcomponent.SymmetricComponent;
import net.sf.openrocket.startup.Application;
import net.sf.openrocket.unit.UnitGroup;
import net.sf.openrocket.util.Coordinate;
import net.sf.openrocket.util.MathUtil;

/**
 * An optimization parameter that computes either the absolute or relative stability of a rocket.
 * 
 * @author Sampo Niskanen <sampo.niskanen@iki.fi>
 */
public class StabilityParameter implements OptimizableParameter {
	
	private static final Logger log = LoggerFactory.getLogger(StabilityParameter.class);
	private static final Translator trans = Application.getTranslator();
	
	
	private final boolean absolute;
	
	public StabilityParameter(boolean absolute) {
		this.absolute = absolute;
	}
	
	
	@Override
	public String getName() {
		return trans.get("name") + " (" + getUnitGroup().getDefaultUnit().getUnit() + ")";
	}
	
	@Override
	public double computeValue(Simulation simulation) throws OptimizationException {
		log.debug("Calculating stability of simulation, absolute=" + absolute);

		/*
		 * These are instantiated each time because this class must be thread-safe.
		 * Caching would in any case be inefficient since the rocket changes all the time.
		 */
		final AerodynamicCalculator aerodynamicCalculator = new BarrowmanCalculator();
		final FlightConfiguration configuration = simulation.getActiveConfiguration();
		final FlightConditions conditions = new FlightConditions(configuration);
		conditions.setMach(Application.getPreferences().getDefaultMach());
		conditions.setAOA(0);
		conditions.setRollRate(0);
		
		final Coordinate cp = aerodynamicCalculator.getWorstCP(configuration, conditions, null);
		// the launch CM is the worst case CM
		final Coordinate cg = MassCalculator.calculateLaunch(configuration).getCM();

		double cpx = Double.NaN;
		if (cp.weight > 0.000001)
			cpx = cp.x;

		double cgx = Double.NaN;
		if (cg.weight > 0.000001)
			cgx = cg.x;
		
		// Calculate the reference (absolute or relative)
		final double stability_absolute = cpx - cgx;

		if (absolute) {
			return stability_absolute;
		} else {
			double diameter = 0;
			for (RocketComponent c : configuration.getActiveInstances().keySet()) {
				if (c instanceof SymmetricComponent) {
					final double d1 = ((SymmetricComponent) c).getForeRadius() * 2;
					final double d2 = ((SymmetricComponent) c).getAftRadius() * 2;
					diameter = MathUtil.max(diameter, d1, d2);
				}
			}
			return stability_absolute / diameter;
		}
	}
	
	@Override
	public UnitGroup getUnitGroup() {
		if (absolute) {
			return UnitGroup.UNITS_LENGTH;
		} else {
			return UnitGroup.UNITS_STABILITY_CALIBERS;
		}
	}
	
}
