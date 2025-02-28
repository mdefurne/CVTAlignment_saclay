package org.jlab.rec.cvt.cluster;

import java.util.ArrayList;

import javax.vecmath.Point3d;

import org.jlab.geom.prim.Point3D;
import org.jlab.rec.cvt.hit.FittedHit;
import org.jlab.rec.cvt.hit.Hit;

/**
 * A cluster in the BST consists of an array of hits that are grouped together
 * according to the algorithm of the ClusterFinder class
 *
 * @author ziegler
 *
 */
public class Cluster extends ArrayList<FittedHit> implements Comparable<Cluster> {

    private static final long serialVersionUID = 9153980362683755204L;

    private int _Detector;							//              The detector SVT or BMT
    private int _DetectorType;                                                  //              The detector type  for BMT C or Z
    private int _Sector;      							//	        sector[1...]
    private int _Layer;    	 						//	        layer [1,...]
    private int _Id;								//		cluster Id
    private int _StripTmin;
    private int _StripTmin_second;
    private int _StripTmax;
    private double _Centroid; 							// 		after LC (Lorentz Correction)
    private double _CentroidError;
    private double _Centroid0; 							// 		before LC
    private double _TotalEnergy;
    private double _Phi;  							// 		for Z-detectors
    private double _PhiErr;
    private double _Phi0;  							// 		for Z-detectors before LC
    private double _PhiErr0;
    private double _Z;    							// 		for C-detectors
    private double _ZErr;
    private double _X;
    private double _XErr;
    private double _Y;
    private double _YErr;
    private float _Tmin;
    private float _Tmin_second;
    private float _Tmax;

    public Cluster(int detector, int detectortype, int sector, int layer, int cid) {
        this._Detector = detector;
        this._DetectorType = detectortype;
        this._Sector = sector;
        this._Layer = layer;
        this._Id = cid;
        this._Tmin= 10000;
        this._Tmin_second= 11000;
        this._Tmax=0;
        this._StripTmin=-1;
        this._StripTmin_second=-1;
        this._StripTmax=-1;
    }

    /**
     *
     * @param hit the first hit in the list of hits composing the cluster
     * @param cid the id of the cluster
     * @return an array list of hits characterized by its sector, layer and id
     * number.
     */
    public Cluster newCluster(Hit hit, int cid) {
        return new Cluster(hit.get_Detector(), hit.get_DetectorType(), hit.get_Sector(), hit.get_Layer(), cid);
    }

    public int get_Detector() {
        return _Detector;
    }

    public void set_Detector(int _Detector) {
        this._Detector = _Detector;
    }

    public int get_DetectorType() {
        return _DetectorType;
    }

    public void set_DetectorType(int _DetectorType) {
        this._DetectorType = _DetectorType;
    }

    /**
     *
     * @return the sector of the cluster
     */
    public int get_Sector() {
        return _Sector;
    }

    /**
     *
     * @param _Sector sector of the cluster
     */
    public void set_Sector(int _Sector) {
        this._Sector = _Sector;
    }

    /**
     *
     * @return the layer of the cluster
     */
    public int get_Layer() {
        return _Layer;
    }

    /**
     *
     * @param _Layer the layer of the cluster
     */
    public void set_Layer(int _Layer) {
        this._Layer = _Layer;
    }

    /**
     *
     * @return the id of the cluster
     */
    public int get_Id() {
        return _Id;
    }

    /**
     *
     * @param _Id the id of the cluster
     */
    public void set_Id(int _Id) {
        this._Id = _Id;
    }

    /**
     *
     * @return region (1...4)
     */
    public int get_Region() {
        return (int) (this._Layer + 1) / 2;
    }

    /**
     *
     * @return superlayer 1 or 2 in region (1...4)
     */
    public int get_RegionSlayer() {
        return (this._Layer + 1) % 2 + 1;
    }

    /**
     * sets energy-weighted parameters; these are the strip centroid
     * (energy-weighted) value, the energy-weighted phi for Z detectors and the
     * energy-weighted z for C detectors
     */
    public void calc_CentroidParams( org.jlab.rec.cvt.bmt.Geometry bgeo) {
        // instantiation of variables
        double stripNumCent = 0;		// cluster Lorentz-angle-corrected energy-weighted strip = centroid
        double stripNumCent0 = 0;		// cluster uncorrected energy-weighted strip = centroid
        double phiCent = 0;			// cluster Lorentz-angle-corrected energy-weighted phi
        double phiErrCent = 0;			// cluster Lorentz-angle-corrected energy-weighted phi error
        double phiCent0 = 0;			// cluster uncorrected energy-weighted phi
        double phiErrCent0 = 0;			// cluster uncorrected energy-weighted phi error
        double xCent = 0;			// cluster energy-weighted x
        double xErrCent = 0;			// cluster energy-weighted x error
        double yCent = 0;			// cluster energy-weighted y
        double yErrCent = 0;			// cluster energy-weighted y error
        double zCent = 0;			// cluster energy-weighted z
        double zErrCent = 0;			// cluster energy-weighted z error
        double totEn = 0.;			// cluster total energy
        double weightedStrp = 0;		// Lorentz-angle-corrected energy-weighted strip 
        double weightedStrp0 = 0;		// uncorrected energy-weighted strip 
        double weightedPhi = 0;			// Lorentz-angle-corrected energy-weighted phi of the strip 
        double weightedPhiErrSq = 0;    // Err^2 on Lorentz-angle-corrected energy-weighted phi of the strip 
        double weightedPhi0 = 0;		// Uncorrected energy-weighted phi of the strip 
        double weightedPhiErrSq0 = 0;   // Err^2 on uncorrected energy-weighted phi of the strip 
        double weightedX = 0;			// Energy-weighted x of the strip
        double weightedXErrSq = 0;		// Err^2 on  energy-weighted x of the strip
        double weightedY = 0;			// Energy-weighted y of the strip
        double weightedYErrSq = 0;		// Err^2 on  energy-weighted y of the strip
        double weightedZ = 0;			// Energy-weighted z of the strip
        double weightedZErrSq = 0;		// Err^2 on  energy-weighted z of the strip

        int nbhits = this.size();

        if (nbhits != 0) {
            int min = 1000000;
            int max = -1;
            int seed = -1;
            double Emax = -1;
            // looping over the number of hits in the cluster
            for (int i = 0; i < nbhits; i++) {
                FittedHit thehit = this.get(i);
                // gets the energy value of the strip
                double strpEn = thehit.get_Strip().get_Edep();
                if (this._Tmin>=thehit.get_Strip().get_Time()) {
                	this._StripTmin_second=this._StripTmin;
                	this._Tmin_second=this._Tmin;
                	this._Tmin=thehit.get_Strip().get_Time();
                	this._StripTmin=i;
                } else {
                	 if (this._Tmin_second>thehit.get_Strip().get_Time()) {
                		 this._Tmin_second=thehit.get_Strip().get_Time();
                     	 this._StripTmin_second=i; 
                	 }
                }
                if (this._Tmax<thehit.get_Strip().get_Time()) {
                	this._Tmax=thehit.get_Strip().get_Time();
                	this._StripTmax=i;
                	//this._StripTmax=thehit.get_Strip().get_Strip();
                }
                int strpNb = 0;
                int strpNb0 = 0; //before LC
                totEn+=strpEn;
                if (this.get_Detector()==0) {
                    // for the SVT the analysis only uses the centroid
                    strpNb = thehit.get_Strip().get_Strip();
                    weightedX+=strpEn*thehit.get_Strip().get_MidPoint().x();
                    weightedY+=strpEn*thehit.get_Strip().get_MidPoint().y();
                    weightedZ+=strpEn*thehit.get_Strip().get_MidPoint().z();
                    weightedXErrSq += strpEn*Math.abs(thehit.get_Strip().get_MidPoint().x()-thehit.get_Strip().get_ImplantPoint().x());
                    weightedYErrSq += strpEn*Math.abs(thehit.get_Strip().get_MidPoint().y()-thehit.get_Strip().get_ImplantPoint().y());
                    weightedZErrSq += strpEn*Math.abs(thehit.get_Strip().get_MidPoint().z()-thehit.get_Strip().get_ImplantPoint().z());
                }
                if (this.get_Detector()==1) { 
                    // for the BMT the analysis distinguishes between C and Z type detectors
                	if (org.jlab.rec.cvt.Constants.ClusteringMode.equals("ADC")) {
                		if (this.get_DetectorType()==0) { // C-detectors
                			strpNb = thehit.get_Strip().get_Strip();
                			// for C detector the Z of the centroid is calculated
                			weightedZ += strpEn * thehit.get_Strip().get_Z();
                			weightedZErrSq += strpEn * (thehit.get_Strip().get_ZErr()) * (thehit.get_Strip().get_ZErr());
                		}
                    	if (this.get_DetectorType()==1) { // Z-detectors
                    		// for Z detectors Larentz-correction is applied to the strip
                    		strpNb = thehit.get_Strip().get_LCStrip();
                    		strpNb0 = thehit.get_Strip().get_Strip();
                    		// for C detectors the phi of the centroid is calculated for the uncorrected and the Lorentz-angle-corrected centroid
                    		weightedPhi += strpEn * thehit.get_Strip().get_Phi();
                    		weightedPhiErrSq += strpEn * (thehit.get_Strip().get_PhiErr()) * (thehit.get_Strip().get_PhiErr());
                    		weightedPhi0 += strpEn * thehit.get_Strip().get_Phi0();
                    		weightedPhiErrSq0 += strpEn * (thehit.get_Strip().get_PhiErr0()) * (thehit.get_Strip().get_PhiErr0());
                    	}
                	}
                }

                if (this.get_Detector()==0||org.jlab.rec.cvt.Constants.ClusteringMode.equals("ADC"))  {
                	weightedStrp += strpEn * (double) strpNb;
                	weightedStrp0 += strpEn * (double) strpNb0;
                }
                
                // getting the max and min strip number in the cluster
                if (strpNb <= min) {
                	min = strpNb;
                }
                if (strpNb >= max) {
                	max = strpNb;
                }
                	// getting the seed strip which is defined as the strip with the largest deposited energy
                if (strpEn >= Emax) {
                	Emax = strpEn;
                	seed = strpNb;
                	if (this.get_DetectorType()==1) {
                		seed = strpNb0;
                	}
                }
                

            }
            if (totEn == 0&&(this.get_Detector()==0||org.jlab.rec.cvt.Constants.ClusteringMode.equals("ADC"))) {
                System.err.println(" Cluster energy is null .... exit "+this._Detector+" "+this._DetectorType);
                
                return;
            }
            _TotalEnergy = totEn; //We change totEn in case of clustering algo for MVT based on time... However it is more relvant for further MVT studies to keep real totEn
            
            if (org.jlab.rec.cvt.Constants.ClusteringMode.equals("Time")&&this.get_Detector()==1) {
            	  int strpNb = 0;
                  int strpNb0 = 0; //before LC
                  double totEn_Time=0;
                  double strpEn = this.get(this._StripTmin).get_Strip().get_Edep();
                  totEn_Time+=strpEn;
            	if (this.get_DetectorType()==0) { // C-detectors
        			strpNb = this.get(this._StripTmin).get_Strip().get_Strip();
        			// for C detector the Z of the centroid is calculated
        			weightedZ = strpEn * this.get(this._StripTmin).get_Strip().get_Z();
        			weightedZErrSq = strpEn * (this.get(this._StripTmin).get_Strip().get_ZErr()) * (this.get(this._StripTmin).get_Strip().get_ZErr());
        			weightedStrp += strpEn * (double) strpNb;
        			if (this._StripTmin_second!=-1) {
        				strpEn = this.get(this._StripTmin_second).get_Strip().get_Edep();
        				totEn_Time+=strpEn;
        				weightedStrp += strpEn * (double) strpNb;
        				weightedZ += strpEn * this.get(this._StripTmin_second).get_Strip().get_Z();
            			weightedZErrSq += strpEn * (this.get(this._StripTmin_second).get_Strip().get_ZErr()) * (this.get(this._StripTmin_second).get_Strip().get_ZErr());
        			}
        		}
            	if (this.get_DetectorType()==1) { // Z-detectors
            		// for Z detectors Larentz-correction is applied to the strip
            		strpNb = this.get(this._StripTmin).get_Strip().get_LCStrip();
            		strpNb0 = this.get(this._StripTmin).get_Strip().get_Strip();
            		weightedStrp += strpEn * (double) strpNb;
                	weightedStrp0 += strpEn * (double) strpNb0;
            		// for C detectors the phi of the centroid is calculated for the uncorrected and the Lorentz-angle-corrected centroid
            		weightedPhi += strpEn * this.get(this._StripTmin).get_Strip().get_Phi();
            		weightedPhiErrSq += strpEn * (this.get(this._StripTmin).get_Strip().get_PhiErr()) * (this.get(this._StripTmin).get_Strip().get_PhiErr());
            		weightedPhi0 += strpEn * this.get(this._StripTmin).get_Strip().get_Phi0();
            		weightedPhiErrSq0 += strpEn * (this.get(this._StripTmin).get_Strip().get_PhiErr0()) * (this.get(this._StripTmin).get_Strip().get_PhiErr0());
            		if (this._StripTmin_second!=-1) {
            			strpEn = this.get(this._StripTmin_second).get_Strip().get_Edep();
            			strpNb = this.get(this._StripTmin_second).get_Strip().get_LCStrip();
                		strpNb0 = this.get(this._StripTmin_second).get_Strip().get_Strip();
            			totEn_Time+=strpEn;
            			weightedStrp += strpEn * (double) strpNb;
                    	weightedStrp0 += strpEn * (double) strpNb0;
            			weightedPhi += strpEn * this.get(this._StripTmin_second).get_Strip().get_Phi();
                		weightedPhiErrSq += strpEn * (this.get(this._StripTmin_second).get_Strip().get_PhiErr()) * (this.get(this._StripTmin_second).get_Strip().get_PhiErr());
                		weightedPhi0 += strpEn * this.get(this._StripTmin_second).get_Strip().get_Phi0();
                		weightedPhiErrSq0 += strpEn * (this.get(this._StripTmin_second).get_Strip().get_PhiErr0()) * (this.get(this._StripTmin_second).get_Strip().get_PhiErr0());
        			}
            	}
            	totEn=totEn_Time;
        	}

            this.set_MinStrip(min);
            this.set_MaxStrip(max);
            this.set_SeedStrip(seed);
            this.set_SeedEnergy(Emax);
            // calculates the centroid values and associated errors
            stripNumCent = weightedStrp / totEn;
            stripNumCent0 = weightedStrp0 / totEn;
            //phiCent = geo.LorentzAngleCorr(phiCent0,this.get_Layer());
            phiCent0 = weightedPhi0 / totEn;
            phiCent = weightedPhi / totEn;
            xCent = weightedX / totEn;
            yCent = weightedY / totEn;
            zCent = weightedZ / totEn;
            phiErrCent = Math.sqrt(weightedPhiErrSq/totEn);
            phiErrCent0 = Math.sqrt(weightedPhiErrSq0/totEn);
            xErrCent = Math.sqrt(weightedXErrSq/totEn);
            yErrCent = Math.sqrt(weightedYErrSq/totEn);
            zErrCent = Math.sqrt(weightedZErrSq/totEn);

            //phiErrCent = Math.sqrt(weightedPhiErrSq);
            //phiErrCent0 = Math.sqrt(weightedPhiErrSq0);
            //zErrCent = Math.sqrt(weightedZErrSq);
        }

        
        _Centroid = stripNumCent;
        if (this.get_Detector()==0) {
        	_X=xCent;
        	_Y=yCent;
        	_Z=zCent;
        	_XErr=xErrCent+2;
        	_YErr=yErrCent+2;
        	_ZErr=zErrCent;
        	
        }
        if (this.get_DetectorType() == 1&&this.get_Detector()==1) {
            set_Centroid0(stripNumCent0);
            set_Centroid(stripNumCent0);
            _Phi = bgeo.LorentzAngleCorr(phiCent0,this.get_Layer(), this.get_Sector(), org.jlab.rec.cvt.Constants.ClusteringMode);
            _PhiErr = phiErrCent;
            _Z=Double.NaN;
            _ZErr=Double.NaN;
            set_Phi0(phiCent0);
            set_PhiErr0(phiErrCent0);
            if (org.jlab.rec.cvt.Constants.ClusteringMode.equals("Time")&&this.size()>1&&this._StripTmin_second==-1) {
            	 System.err.println(" Bad cluster centroid in time mode .... exit "+this._Detector+" "+this._DetectorType);
                 
                 return;
            }
        }
        
        if (this.get_DetectorType() == 0&&this.get_Detector()==1) {
        	_X=Double.NaN;
        	_Y=Double.NaN;
        	_XErr=Double.NaN;
        	_YErr=Double.NaN;
            _Z = zCent;
            _ZErr = zErrCent;
        }

    }

    public double get_Centroid() {
        return _Centroid;
    }

    public void set_Centroid(double _Centroid) {
        this._Centroid = _Centroid;
    }
    
    public double get_CentroidError() {
        return _CentroidError;
    }

    public void set_CentroidError(double _CentroidE) {
        this._CentroidError = _CentroidE;
    }
    public double get_Centroid0() {
        return _Centroid0;
    }

    public void set_Centroid0(double _Centroid0) {
        this._Centroid0 = _Centroid0;
    }

    public double get_Phi() {
        return _Phi;
    }

    public void set_Phi(double _Phi) {
        this._Phi = _Phi;
    }

    public double get_Phi0() {
        return _Phi0;
    }

    public void set_Phi0(double _Phi0) {
        this._Phi0 = _Phi0;
    }

    public double get_PhiErr() {
        return _PhiErr;
    }

    public void set_PhiErr(double _PhiErr) {
        this._PhiErr = _PhiErr;
    }

    public double get_PhiErr0() {
        return _PhiErr0;
    }

    public void set_PhiErr0(double _PhiErr0) {
        this._PhiErr0 = _PhiErr0;
    }

    public double get_Z() {
        return _Z;
    }

    public void set_Z(double _Z) {
        this._Z = _Z;
    }

    public double get_ZErr() {
        return _ZErr;
    }

    public void set_ZErr(double _ZErr) {
        this._ZErr = _ZErr;
    }
    
    public double get_X() {
        return _X;
    }

    public void set_X(double _X) {
        this._X = _X;
    }

    public double get_XErr() {
        return _XErr;
    }

    public void set_XErr(double _XErr) {
        this._XErr = _XErr;
    }
    
    public double get_Y() {
        return _Y;
    }

    public void set_Y(double _Y) {
        this._Y = _Y;
    }

    public double get_YErr() {
        return _YErr;
    }

    public void set_YErr(double _YErr) {
        this._YErr = _YErr;
    }


    public double get_TotalEnergy() {
        return _TotalEnergy;
    }
    
    public float get_Tmin() {
    	return _Tmin;
    }
    
    public float get_Tmax() {
    	return _Tmax;
    }
    
    public int get_StripTmin() {
    	return _StripTmin;
    }
    
    public int get_StripTmax() {
    	return _StripTmax;
    }

    public void set_TotalEnergy(double _TotalEnergy) {
        this._TotalEnergy = _TotalEnergy;
    }

    private int _MinStrip;			// the min strip number in the cluster
    private int _MaxStrip;			// the max strip number in the cluster
    private int _SeedStrip;			// the seed: the strip with largest deposited energy
    private double _SeedEnergy;                 // the deposited energy of the seed

    private double _SeedResidual;               // residual is doca to seed strip from trk intersection with module plane
    private double _CentroidResidual;           // residual is doca to centroid of cluster to trk inters with module plane

    public int get_MinStrip() {
        return _MinStrip;
    }

    public void set_MinStrip(int _MinStrip) {
        this._MinStrip = _MinStrip;
    }

    public int get_MaxStrip() {
        return _MaxStrip;
    }

    public void set_MaxStrip(int _MaxStrip) {
        this._MaxStrip = _MaxStrip;
    }

    public int get_SeedStrip() {
        return _SeedStrip;
    }

    public void set_SeedStrip(int _SeedStrip) {
        this._SeedStrip = _SeedStrip;
    }

    public double get_SeedEnergy() {
        return _SeedEnergy;
    }

    public void set_SeedEnergy(double _SeedEnergy) {
        this._SeedEnergy = _SeedEnergy;
    }

    public double get_SeedResidual() {
        return _SeedResidual;
    }

    public void set_SeedResidual(double _SeedResidual) {
        this._SeedResidual = _SeedResidual;
    }

    public double get_CentroidResidual() {
        return _CentroidResidual;
    }

    public void set_CentroidResidual(double _CentroidResidual) {
        this._CentroidResidual = _CentroidResidual;
    }

    /**
     *
     * @return cluster info. about location and number of hits contained in it
     */
    public void printInfo() {
        String s = " cluster: Detector " + this.get_Detector() +"  Detector Type " + this.get_DetectorType() + " ID " + this.get_Id() + " Sector " + this.get_Sector() + " Layer " + this.get_Layer() + " Size " + this.size() +" centroid "+this.get_Centroid();
        System.out.println(s);
    }

    /**
     *
     * @param Z z-coordinate of a point in the local coordinate system of a
     * module
     * @return the average resolution for a group of strips in a cluster in the
     * SVT
     *
     */
    public double get_ResolutionAlongZ(double Z, org.jlab.rec.cvt.svt.Geometry geo) {

        // returns the total resolution for a group of strips in a cluster
        // the single strip resolution varies at each point along the strip as a function of Z (due to the graded angle of the strips) and 
        // is smallest at the pitch implant at which is is simply Pitch/sqrt(12)
        int nbhits = this.size();
        if (nbhits == 0) {
            return 0;
        }

        // average
        double res = 0;

        for (int i = 0; i < nbhits; i++) {
            double rstrp = geo.getSingleStripResolution(this.get(i).get_Layer(), this.get(i).get_Strip().get_Strip(), Z);
            res += rstrp * rstrp;
        }
        return Math.sqrt(res);
    }

    private int AssociatedTrackID = -1; // the track ID associated with that hit

    public int get_AssociatedTrackID() {
        return AssociatedTrackID;
    }

    public void set_AssociatedTrackID(int associatedTrackID) {
        AssociatedTrackID = associatedTrackID;
    }

    @Override
    public int compareTo(Cluster arg) {
            
        //sort by phi of strip implant of first strip in the cluster, then by layer, then by seed strip number
        double this_phi = PhiInRange(this.get(0).get_Strip().get_ImplantPoint().toVector3D().phi());
        double arg_phi = PhiInRange(arg.get(0).get_Strip().get_ImplantPoint().toVector3D().phi());

        int CompPhi = this_phi < arg_phi ? -1 : this_phi == arg_phi ? 0 : 1;
        int CompLay = this._Layer < arg._Layer ? -1 : this._Layer == arg._Layer ? 0 : 1;
        int CompId = this._SeedStrip < arg._SeedStrip ? -1 : this._SeedStrip == arg._SeedStrip ? 0 : 1;

        int return_val1 = ((CompLay == 0) ? CompId : CompLay);
        int return_val = ((CompPhi == 0) ? return_val1 : CompPhi);

        return return_val;
        
        
    }

    private double PhiInRange(double phi) {
        if (phi < 0) {
            phi += Math.PI * 2;
        }
        return phi;
    }

    
}
