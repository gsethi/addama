package org.systemsbiology.addama.gaesvcs.refgenome;
import java.io.OutputStream;

/**
 * org.sysgemsbiology.genome.IReferenceGenomeServer
 * User: steven
 * Date: May 25, 2010
 */
public interface ReferenceGenomeServer {
    /**
     * provides a list of the chromosomes for this build
     *
     * @return chromosomes - string[]
     */
    public String[] getChromosomes();

    /**
     * Returns the length of the chromosome
     *
     * @param chromosome - String
     * @return length - Long
     * @throws Exception - errors reading data
     */
    public Long getChromosomeLength(String chromosome) throws Exception;

    /**
     * write the sequence (AGGCTAA...) for the given chromosome and range into the provided output stream
     *
     * @param outputStream output stream
     * @param chromosome   chromosome name
     * @param start        sequence start
     * @param end          sequence end
     * @throws Exception - errors reading data
     */
    public void loadSequence(OutputStream outputStream, String chromosome, long start, long end) throws Exception;

}
