package hu.lanoga.toolbox.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MetricConverterUtil {
	
	public static final BigDecimal KG_TO_LB = new BigDecimal("2.20462");
	public static final BigDecimal LB_TO_KG = new BigDecimal("0.45359");
	
	public static final BigDecimal KM_TO_MILE = new BigDecimal("0.621371192");
	public static final BigDecimal MILE_TO_KG = new BigDecimal("1.609344");

	public static final BigDecimal LITRE_TO_FT = new BigDecimal("0.0353147");
	public static final BigDecimal FT_TO_LITRE = new BigDecimal("28.3168");
	
	public static final BigDecimal TROY_OUNCE_TO_GRAM = new BigDecimal("31.1035");
	
	public static BigDecimal kgToLb(BigDecimal weightInKg) {
		
		if (weightInKg == null) {
			return null;
		}
		
		return KG_TO_LB.multiply(weightInKg);
	}

	public static BigDecimal lbToKg(BigDecimal weightInLbs) {
		
		if (weightInLbs == null) {
			return null;
		}
		
		return LB_TO_KG.multiply(weightInLbs);
	}
	
	public static BigDecimal kmToMile(BigDecimal distInKm) {
		
		if (distInKm == null) {
			return null;
		}
		
		return KM_TO_MILE.multiply(distInKm);
	}
	
	public static BigDecimal mileToKm(BigDecimal distInMile) {
		
		if (distInMile == null) {
			return null;
		}
		
		return MILE_TO_KG.multiply(distInMile);
	}
	
	public static BigDecimal troyOunceToGram(BigDecimal weightInTroyOunce) {
		
		if (weightInTroyOunce == null) {
			return null;
		}
		
		return TROY_OUNCE_TO_GRAM.multiply(weightInTroyOunce);
	}
	
	public static BigDecimal gramToTroyOunce(BigDecimal weightInGram) {
		
		if (weightInGram == null) {
			return null;
		}
		
		return weightInGram.setScale(15, RoundingMode.HALF_UP).divide(TROY_OUNCE_TO_GRAM, RoundingMode.HALF_UP); // láasd Excel-ben "Although Excel can display 30 decimal places, its precision for a specified number is confined to 15 significant figures, and calculations may have an accuracy that is even less due to three issues: round off,[3] truncation, and binary storage."
	}

	public static BigDecimal litreToFt(BigDecimal capacityInLitre) {

		if (capacityInLitre == null) {
			return null;
		}

		return LITRE_TO_FT.multiply(capacityInLitre);
	}

	public static BigDecimal ftToLitre(BigDecimal capacityInFt) {

		if (capacityInFt == null) {
			return null;
		}

		return FT_TO_LITRE.multiply(capacityInFt);
	}
	
	private MetricConverterUtil() {
		//
	}

}
