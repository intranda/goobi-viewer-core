package de.intranda.digiverso.presentation.controller.language;


public class Language {

    private String isoCode;
    private String isoCodeOld;
    private String englishName;
    private String frenchName;
    private String germanName;
    /**
     * @return the language code according to iso 639-2/B
     */
    public String getIsoCode() {
        return isoCode;
    }
    /**
     * @param the language code according to iso 639-2/B
     */
    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }
    /**
     * @return the language code according to iso 639-1
     */
    public String getIsoCodeOld() {
        return isoCodeOld;
    }
    /**
     * @param the language code according to iso 639-1
     */
    public void setIsoCodeOld(String isoCodeOld) {
        this.isoCodeOld = isoCodeOld;
    }
    /**
     * @return the englishName
     */
    public String getEnglishName() {
        return englishName;
    }
    /**
     * @param englishName the englishName to set
     */
    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }
    /**
     * @return the frenchName
     */
    public String getFrenchName() {
        return frenchName;
    }
    /**
     * @param frenchName the frenchName to set
     */
    public void setFrenchName(String frenchName) {
        this.frenchName = frenchName;
    }
    /**
     * @return the germanName
     */
    public String getGermanName() {
        return germanName;
    }
    /**
     * @param germanName the germanName to set
     */
    public void setGermanName(String germanName) {
        this.germanName = germanName;
    }
    
    
}
