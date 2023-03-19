package hu.lanoga.toolbox.export;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import hu.lanoga.toolbox.export.pdf.advanced.AdvancedPdfExporterUtil;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExporterOptions {

	/**
	 * amennyiben meg van adva, akkor automatikusan az {@link AdvancedPdfExporterUtil} kerül alkalmazásra (PDF exportnál) 
	 * az itt megadott template fájlt (kell a végére a .vm kiterjesztés!) használva... (AdvancedPdfExporterUtil Java -> HTML (Velocity) -> PDF alapú) 
	 * 
	 * @return
	 */
    String advancedPdfTemplateFile();

}