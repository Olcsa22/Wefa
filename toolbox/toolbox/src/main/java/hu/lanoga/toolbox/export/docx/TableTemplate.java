package hu.lanoga.toolbox.export.docx;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class TableTemplate {
		
    /**
     * TT nyitó tag paragraph id-je
     */
    private int startP;

    /**
     * TT záró tag paragraph id-je
     */
    private int endP;

    /**
     * TT adatszerkezet map-beli id-je
     */
    private String mapKey;

    /**
     * header row count: első hrc db sor rögzítve
     */
    private int hrc;

    /**
     * footer row count: utolsó frc db sor rögzítve
     */
    private int frc;

}