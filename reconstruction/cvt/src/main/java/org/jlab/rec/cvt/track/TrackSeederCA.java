package org.jlab.rec.cvt.track;
import org.jlab.rec.cvt.track.MakerCA;
import org.jlab.rec.cvt.track.Cell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jlab.clas.swimtools.Swim;

import org.jlab.geom.prim.Point3D;
import org.jlab.rec.cvt.CentralTracker;
import org.jlab.rec.cvt.cluster.Cluster;
import org.jlab.rec.cvt.cross.Cross;
import org.jlab.rec.cvt.fit.CircleFitter;
import org.jlab.rec.cvt.fit.HelicalTrackFitter;
import org.jlab.rec.cvt.fit.LineFitPars;
import org.jlab.rec.cvt.fit.LineFitter;
import org.jlab.rec.cvt.Constants;


public class TrackSeederCA {

    public TrackSeederCA() {
        
    }



    // Retrieve lists of crosses as track candidates
    // from the output of the cellular automaton   
    // it looks only for the maximum state, TODO: remove found candidate and continue
    public List<ArrayList<Cross>> getCAcandidates( List<Cell> nodes, Swim swimmer ) {
//System.out.println("\n\n\t ____inside get candidates___");
        List<ArrayList<Cross>> trCands = new ArrayList<ArrayList<Cross>>();
        List<ArrayList<Cell>> cellCands = new ArrayList<ArrayList<Cell>>();
        if( nodes.size() == 0 ) return trCands;
        Collections.sort( nodes );
        int mstate = nodes.get(0).get_state();
        // get list of cells that are good starting candidates
        // - they have to be at least of Nmax - 2 in score
        // - they have to be terminals: no parents
        ArrayList<Cell> starts = new ArrayList<Cell>();
        for( Cell c : nodes ) {
        	if( c.get_state() >= (mstate-2) ) { 
        		if ( c.getParent() == null ) {
        			
        			if( c.get_state() == mstate) {
        				c.get_c2().set_usedInXYcand( true );
        			}
        			
        			starts.add(c);
            //System.out.println( c );
        		}
        	}
        	else {
        		break; // they are sorted
        	}
        }
        
        for( Cell start : starts ) {
        	
        	if( start.get_state() != mstate  ) {
        		if( start.get_c2().is_usedInXYcand() ) continue;
        	}
        	
        	// queue. TODO: use utils.collections??
        	ArrayList<Cell> Q = new ArrayList<Cell>();
        	Q.add( start );
        	
//        	System.out.println( "\n ==== start " + start);
        	
          // store all the candidates for this starting node
          // then chose the longest ones
          List<ArrayList<Cell>> tempCands = new ArrayList<ArrayList<Cell>>();

          // loop over the queue
        	while( Q.size() > 0 ) {
        		
        		Cell cc = Q.remove( Q.size() - 1 );
            //System.out.println( cc );
        		for( Cell nn : cc.get_neighbors() ) {
        			if( nn.get_state() == cc.get_state() - 1 ){
        			  nn.setParent( cc );
        			  Q.add( nn );
              		}
        		}
        		
            // if the cell has no neighbours, then look backward at the parents to form a candidate
        		if( cc.get_neighbors().size() == 0 ) {
        			ArrayList<Cell> tcand = new ArrayList<Cell>();
        			tcand.add(cc);
        			Cell pc = cc.getParent();
        			while( pc != null ) {
        				tcand.add(pc);
        				pc = pc.getParent();
        			}
        			
        			if( tcand.size() > 0 ) {
        				if( tcand.get(0).get_plane().equalsIgnoreCase( "XY" ) )
        				{
        					if( tcand.size() > 3 )
        						tempCands.add(tcand);
        				}
        				else {
    						  tempCands.add(tcand);
        				}
        			}
        		}
        	}
          // once all the candidates are found chose the longest
//System.out.println( " ---  " + start + " \t \t " + tempCands.size() );

          // find the max size
          int l = 0;
          for( ArrayList<Cell> cand : tempCands ){
            if( cand.size() > l ) l = cand.size();
          }

         // add all the max size candidates to the list
          int CC = 0;
          for( ArrayList<Cell> cand : tempCands ){
            if( cand.size() == l ) { CC++;  cellCands.add(  cand ); 
//            	for( Cell c : cand ) {
//            		System.out.println(c);
//            		System.out.println("     " + c.get_c1());
//            		System.out.println("     " + c.get_c2());
//            	}
            }
          }
//System.out.println( " l: " + l + "   CC: " + CC );
      
        }

        
//        System.out.println(" cellCands " + cellCands.size() );
         
        for( List<Cell> candcell : cellCands ){
          if(candcell.size() == 0 ) continue;
      	  trCands.add( getCrossFromCells(candcell));
//      	  trCands.add( new ArrayList<Cross>());
//  		  trCands.get(trCands.size()-1).add( candcell.get(0).get_c2() );
//      	  for( Cell c : candcell ){
//      		  trCands.get(trCands.size()-1).add( c.get_c1() );
//      	  }
        }
        return trCands;      
    }

    private ArrayList<Cross> getCrossFromCells( List<Cell> l ){
    	if( l == null ) return null;
    	ArrayList<Cross> crs = new ArrayList<Cross>();
    	for( Cell c : l) crs.add(c.get_c1());
    	crs.add( l.get( l.size()-1).get_c2());
    	
//    	System.out.println();
//    	for( Cross c : crs ) System.out.println( "\t\t ... c: " + c);
    	return crs;
    }

    // create and run the cellular automaton
    public List<Cell> runCAMaker( String plane, int nepochs, ArrayList<Cross> crs, 
            org.jlab.rec.cvt.bmt.Geometry bgeom, 
            Swim swimmer, 
        CentralTracker CVT){
        MakerCA camaker = new MakerCA(false);
        camaker.set_plane( plane );
        if( plane.equalsIgnoreCase("XY") ){
          camaker.set_cosBtwCells(0.95);  // min dot product between neighbours 
          camaker.set_abCrs(50);         // max angle between crosses to form a cell
          camaker.set_aCvsR(45);         // max angle between the cell and the radius to the first cell
        }
        if( plane.equalsIgnoreCase("ZR") ){
          camaker.set_cosBtwCells(0.95); // it only applies to the BMTC cross only cells
          camaker.set_abCrs(30.);
          camaker.set_aCvsR(90.);
        }
        
        camaker.createCells(crs, bgeom, CVT);
        camaker.findNeighbors();
//        camaker.evolve( nepochs );
        return camaker.getNodes();  
    }
    
    public List<Seed> findSeed(List<Cross> svt_crosses, List<Cross> bmt_crosses, 
    						   org.jlab.rec.cvt.svt.Geometry svt_geo, 
    						   org.jlab.rec.cvt.bmt.Geometry bmt_geo, 
                                                   Swim swimmer, 
                    CentralTracker CVT) {
       
        List<Seed> seedlist = new ArrayList<Seed>();

        ArrayList<Cross> crosses = new ArrayList<Cross>();
        List<ArrayList<Cross>> bmtC_crosses = new ArrayList<ArrayList<Cross>>();
        for( int i=0;i<3;i++) bmtC_crosses.add( new ArrayList<Cross>() );
        
        crosses.addAll(svt_crosses);

//        Collections.sort(crosses);
        
        for(Cross c : bmt_crosses) { 
            if(c.get_DetectorType().equalsIgnoreCase("Z"))
                crosses.add(c);
            if(c.get_DetectorType().equalsIgnoreCase("C")) {
                bmtC_crosses.get(c.get_Sector()-1).add(c);	
            }
        }


        // look for candidates in the XY plane
        // run the cellular automaton over SVT and BMT_Z crosses

        List<Cell> xynodes = runCAMaker( "XY", 10, crosses, bmt_geo, swimmer, CVT); 
        List<ArrayList<Cross>> xytracks =  getCAcandidates( xynodes, swimmer);

//        System.out.println( " XY tracks " + xytracks );
        

        // find ZR candidates and match with XY
        // ------------------------------------
        List<ArrayList<Cross>> seedCrosses = CAonRZ( xytracks, bmtC_crosses, svt_geo, bmt_geo, swimmer, CVT);
        
        List<Track> cands = new ArrayList<Track>();
        for (int s = 0; s < seedCrosses.size(); s++) {
          Collections.sort(seedCrosses.get(s));      // TODO: check why sorting matters
          Track cand = fitSeed(seedCrosses.get(s), svt_geo, 5, false, swimmer);
          
          if (cand != null) {
            cands.add(cand);
          }
        }

        for( Track cand : cands ) {
          //cand.finalUpdate_Crosses(svt_geo); // this should update the Z position, only for display purposes 
            Seed seed = new Seed();
            seed.set_Crosses(cand);
            seed.set_Helix(cand.get_helix());
            seedlist.add(seed);
            List<Cluster> clusters = new ArrayList<Cluster>();
            for(Cross c : seed.get_Crosses()) { 
                if(c.get_Detector().equalsIgnoreCase("SVT")) {
                    clusters.add(c.get_Cluster1());
                   // clusters.add(c.get_Cluster2());
                } else {
                    clusters.add(c.get_Cluster1());
                }
            }
            seed.set_Clusters(clusters);
        }

        return seedlist;
    }
    
    private List<Track> rmDuplicate( List<Track> tracks) {
    	List<Track> goodTrks = new ArrayList<Track>();
    	List<Track> badTrks = new ArrayList<Track>();

    	List<Track> sample = new ArrayList<Track>();
    	List<Track> selected = new ArrayList<Track>();
    	for( int i=0;i<tracks.size();i++) {
    		Track tr = tracks.get(i);
    		if( tr == null ) continue;
    		// check if the track is a bad clone already discarded
    		if( badTrks.contains(tr) ) continue;
    		
    		
    		// look for all the clones. Tracks are considered clones if they share at least 2 crosses
    		sample.clear();
        	for( int j=0;j<tracks.size();j++) {
        		Track tj = tracks.get(j);
        		int nshared = 0;
        		for( Cross c : tj ) {
        			if( tr.contains(c) ) {
        				nshared++;
        				if( nshared >= 2 ) {
                			sample.add(tj);
                			break;
                		}
        			}
        		}
        		
        	}
        	
        	// find the best clone
        	int size = 0;
        	int itr = 0;
        	selected.clear();
        	for( int j=0;j<sample.size();j++) {
        		Track ts = sample.get(j);
        		int tmpsize = ts.size();
        		if( tmpsize >= size ) {
        			size = tmpsize;
        			selected.add(ts);
        		}
        		else {
        			badTrks.add(ts);
        		}
        	}
        	
        	// add the best clone to the good tracks
        	goodTrks.addAll( selected);
        	
        	// remove the bad one from tracks
        	tracks.removeAll(badTrks);
        	
    	}
    	goodTrks.removeAll(badTrks);
    	return goodTrks;
    }
    
    public List<ArrayList<Cross>> CAonRZ( 
                                        List<ArrayList<Cross>>xytracks , 
                                        List<ArrayList<Cross>> bmtC_crosses,
                                        org.jlab.rec.cvt.svt.Geometry svt_geo, 
                                        org.jlab.rec.cvt.bmt.Geometry bmt_geo, 
                                        Swim swimmer, CentralTracker CVT) {
      
      List<ArrayList<Cross>> seedCrosses = new ArrayList<ArrayList<Cross>>();

      if( bmtC_crosses == null ) return null;

      // run CA over BMT_C crosses per sector
      // ---------------------------------------------
      List< List< ArrayList< Cross > > > zrTrksPerSector = new ArrayList<List<ArrayList<Cross>>>();
      for( int i = 0 ; i < 3 ; i++ ){

        List<Cell> zrnodes = runCAMaker( "ZR", 5, bmtC_crosses.get( i ) , bmt_geo, swimmer, CVT);
        List<ArrayList<Cross>> zrtracks =  getCAcandidates( zrnodes, swimmer);
        zrTrksPerSector.add( zrtracks );
      }



      // loop over each xytrack to find ZR candidates
      // ---------------------------------------------
      for( int ixy=0; ixy< xytracks.size();ixy++ ){
        List<Cross> xycross = xytracks.get(ixy);
        ArrayList<Cross> crsZR = new ArrayList<Cross>();
        // get the SVT crosses
        ArrayList<Cross> svtcrs = new ArrayList<Cross>();

        // look for svt crosses and determine the sector from bmt z crosses
        //------------------------------------------------------------------
        int sector = -1;
        double minTime = 0;
        int nBMTxy = 0;
        for( Cross c : xycross ){
          if( c.get_Detector().equalsIgnoreCase("BMT")){
        	  sector = c.get_Sector();
        	  minTime += c.get_Cluster1().get_Tmin();
        	  nBMTxy += 1;
          }
          else {
            svtcrs.add( c );
          }
        }
        if( sector <= 0 ) continue;
        minTime /= nBMTxy;

        List<ArrayList<Cross>> zrtracks =  zrTrksPerSector.get( sector-1 );

        //System.out.println("sector" + sector + " len " + zrtracks.size());
        
        // check if the ZR candidate maches in time
        List<Integer> lrz = new ArrayList<Integer>();
        for( List<Cross> zr : zrtracks ) {
            double minTimeZ = 0;
            int irz = 0;
        	for( Cross c : zr) minTimeZ += c.get_Cluster1().get_Tmin();
        	minTimeZ /= zr.size();
        	
        	if( Math.abs( minTimeZ - minTime ) > Constants.Max_Delta_Tmin   ) {
//        		System.out.println( " ########### removing ZR for mismatch in time: " + minTimeZ + "  " + minTime);
        		lrz.add(irz);
        	}
        	irz++;
        }
        for( int i = lrz.size()-1; i>=0;i--) zrtracks.remove((int)lrz.get(i));
        
        // collect crosses for candidates
        //--------------------------------
        for( List<Cross> zrcross : zrtracks ){
          // FIT ZR BMT
          // ---------------------------
          List<Double> R = new ArrayList<Double>();
          List<Double> Z = new ArrayList<Double>();
          List<Double> EZ= new ArrayList<Double>();
          
          for( Cross c : zrcross ) {
            R.add( org.jlab.rec.cvt.bmt.Constants.getCRCRADIUS()[c.get_Region() - 1] + org.jlab.rec.cvt.bmt.Constants.hStrip2Det );
            Z.add( c.get_Point().z() );
            EZ.add( c.get_PointErr().z());
          }
          
          LineFitter ft = new LineFitter();
          boolean status = ft.fitStatus(Z, R, EZ, null, Z.size());
          if( status == false ) { System.err.println(" BMTC FIT FAILED");}
          LineFitPars fpars = ft.getFit();
          if( fpars == null ) continue;
          double b = fpars.intercept();
          double m = fpars.slope();
                    
          seedCrosses.add( new ArrayList<Cross>() );
          int scsize = seedCrosses.size();
          // add svt
          for( Cross c : svtcrs ) seedCrosses.get(scsize-1).add(c);
          
          // add bmt z
          for( Cross c : xycross ){
            if( c.get_Detector().equalsIgnoreCase("BMT")){
              seedCrosses.get(scsize-1).add(c); 
            }              
          }

            // add bmt c
          for( Cross c : zrcross ){
            if( c.get_Detector().equalsIgnoreCase("BMT")){
              seedCrosses.get(scsize-1).add(c); 
            }
          }
        }
      }

      return seedCrosses;
    }

    private List<Double> X = new ArrayList<Double>();
    private List<Double> Y = new ArrayList<Double>();
    private List<Double> Z = new ArrayList<Double>();
    private List<Double> Rho = new ArrayList<Double>();
    private List<Double> ErrZ = new ArrayList<Double>();
    private List<Double> ErrRho = new ArrayList<Double>();
    private List<Double> ErrRt = new ArrayList<Double>();
    List<Cross> BMTCrossesC = new ArrayList<Cross>();
    List<Cross> BMTCrossesZ = new ArrayList<Cross>();
    List<Cross> SVTCrosses = new ArrayList<Cross>();
   
    public Track fitSeed(List<Cross> VTCrosses, int fitIter, boolean originConstraint, Swim swimmer) {
    	return fitSeed( VTCrosses,null,fitIter,originConstraint, swimmer);
    }
    
    public Track fitSeed(List<Cross> VTCrosses, 
            org.jlab.rec.cvt.svt.Geometry svt_geo, int fitIter, boolean originConstraint,
            Swim swimmer) {
        double chisqMax = Double.POSITIVE_INFINITY;
        
        Track cand = null;
        HelicalTrackFitter fitTrk = new HelicalTrackFitter();
        for (int i = 0; i < fitIter; i++) {
            //	if(originConstraint==true) {
            //		X.add(0, (double) 0);
            //		Y.add(0, (double) 0);
            //		Z.add(0, (double) 0);
            //		Rho.add(0, (double) 0);
            //		ErrRt.add(0, (double) org.jlab.rec.cvt.svt.Constants.RHOVTXCONSTRAINT);
            //		ErrZ.add(0, (double) org.jlab.rec.cvt.svt.Constants.ZVTXCONSTRAINT);		
            //		ErrRho.add(0, (double) org.jlab.rec.cvt.svt.Constants.RHOVTXCONSTRAINT);										
            //	}
            X.clear();
            Y.clear();
            Z.clear();
            Rho.clear();
            ErrZ.clear();
            ErrRho.clear();
            ErrRt.clear();

            int svtSz = 0;
            int bmtZSz = 0;
            int bmtCSz = 0;

            BMTCrossesC.clear();
            BMTCrossesZ.clear();
            SVTCrosses.clear();

            for (Cross c : VTCrosses) {
                if (!(Double.isNaN(c.get_Point().z()) || Double.isNaN(c.get_Point().x()))) {
                    SVTCrosses.add(c);
                }

                if (Double.isNaN(c.get_Point().x())) {
                    BMTCrossesC.add(c);
                }
                if (Double.isNaN(c.get_Point().z())) {
                    BMTCrossesZ.add(c);
                }
            }
            svtSz = SVTCrosses.size();
            if (BMTCrossesZ != null) {
                bmtZSz = BMTCrossesZ.size();
            }
            if (BMTCrossesC != null) {
                bmtCSz = BMTCrossesC.size();
            }

            int useSVTdipAngEst = 1;
            if (bmtCSz >= 2) {
                useSVTdipAngEst = 0;
            }

            ((ArrayList<Double>) X).ensureCapacity(svtSz + bmtZSz);
            ((ArrayList<Double>) Y).ensureCapacity(svtSz + bmtZSz);
            ((ArrayList<Double>) Z).ensureCapacity(svtSz * useSVTdipAngEst + bmtCSz);
            ((ArrayList<Double>) Rho).ensureCapacity(svtSz * useSVTdipAngEst + bmtCSz);
            ((ArrayList<Double>) ErrZ).ensureCapacity(svtSz * useSVTdipAngEst + bmtCSz);
            ((ArrayList<Double>) ErrRho).ensureCapacity(svtSz * useSVTdipAngEst + bmtCSz); // Try: don't use svt in dipdangle fit determination
            ((ArrayList<Double>) ErrRt).ensureCapacity(svtSz + bmtZSz);

            cand = new Track(null, swimmer);
            cand.addAll(SVTCrosses);
            double explFact = 5.0; // 
            for (int j = 0; j < SVTCrosses.size(); j++) {
                X.add(j, SVTCrosses.get(j).get_Point().x());
                Y.add(j, SVTCrosses.get(j).get_Point().y());
                if (useSVTdipAngEst == 1) {
                    Z.add(j, SVTCrosses.get(j).get_Point().z());
                    Rho.add(j, Math.sqrt(SVTCrosses.get(j).get_Point().x() * SVTCrosses.get(j).get_Point().x()
                            + SVTCrosses.get(j).get_Point().y() * SVTCrosses.get(j).get_Point().y()));
                    ErrRho.add(j, explFact *  Math.sqrt(SVTCrosses.get(j).get_PointErr().x() * SVTCrosses.get(j).get_PointErr().x()
                            + SVTCrosses.get(j).get_PointErr().y() * SVTCrosses.get(j).get_PointErr().y()));
                    ErrZ.add(j, explFact *  SVTCrosses.get(j).get_PointErr().z());
                }
                ErrRt.add(j, explFact * Math.sqrt(SVTCrosses.get(j).get_PointErr().x() * SVTCrosses.get(j).get_PointErr().x()
                        + SVTCrosses.get(j).get_PointErr().y() * SVTCrosses.get(j).get_PointErr().y()));
            }

            if (bmtZSz > 0) {
                for (int j = svtSz; j < svtSz + bmtZSz; j++) {
                    X.add(j, BMTCrossesZ.get(j - svtSz).get_Point().x());
                    Y.add(j, BMTCrossesZ.get(j - svtSz).get_Point().y());
                    ErrRt.add(j, explFact * Math.sqrt(BMTCrossesZ.get(j - svtSz).get_PointErr().x() * BMTCrossesZ.get(j - svtSz).get_PointErr().x()
                            + BMTCrossesZ.get(j - svtSz).get_PointErr().y() * BMTCrossesZ.get(j - svtSz).get_PointErr().y()));
                }
            }
            if (bmtCSz > 0) {
                for (int j = svtSz * useSVTdipAngEst; j < svtSz * useSVTdipAngEst + bmtCSz; j++) {
                    Z.add(j, BMTCrossesC.get(j - svtSz * useSVTdipAngEst).get_Point().z());
                    Rho.add(j, org.jlab.rec.cvt.bmt.Constants.getCRCRADIUS()[BMTCrossesC.get(j - svtSz * useSVTdipAngEst).get_Region() - 1]
                            + org.jlab.rec.cvt.bmt.Constants.hStrip2Det);
                    
                    ErrRho.add(j, explFact *  org.jlab.rec.cvt.bmt.Constants.hStrip2Det / Math.sqrt(12.));
                    ErrZ.add(j, explFact *  BMTCrossesC.get(j - svtSz * useSVTdipAngEst).get_PointErr().z());
                }
            }
            X.add((double) 0);
            Y.add((double) 0);

            ErrRt.add((double) 0.5);
//            ErrRt.add((double) 1.5);
                                   
            fitTrk.fit(X, Y, Z, Rho, ErrRt, ErrRho, ErrZ);
            
            if (fitTrk.get_helix() == null) { 
            	return null;
            }

            cand = new Track(fitTrk.get_helix(), swimmer);
            //cand.addAll(SVTCrosses);
            cand.addAll(SVTCrosses);
            cand.addAll(BMTCrossesC);
            cand.addAll(BMTCrossesZ);
            
            cand.set_HelicalTrack(fitTrk.get_helix(), swimmer);
            if( X.size()>3 )
            	cand.set_circleFitChi2PerNDF(fitTrk.get_chisq()[0]/(X.size()-3));
            else 
            	cand.set_circleFitChi2PerNDF(fitTrk.get_chisq()[0]*2); // penalize tracks with only 3 crosses 
            
            if( Z.size() > 2 )
            	cand.set_lineFitChi2PerNDF(fitTrk.get_chisq()[1]/Z.size());
            else
            	cand.set_lineFitChi2PerNDF(fitTrk.get_chisq()[1]*2);// penalize tracks with only 2 crosses
            	
            //if(shift==0)
//            if (fitTrk.get_chisq()[0] < chisqMax) {
//                chisqMax = fitTrk.get_chisq()[0];
//                if(chisqMax<Constants.CIRCLEFIT_MAXCHI2)
//                    cand.update_Crosses(svt_geo);
//                //i=fitIter;
//            }
        }
        //System.out.println(" Seed fitter "+fitTrk.get_chisq()[0]+" "+fitTrk.get_chisq()[1]); 
//        if(chisqMax>Constants.CIRCLEFIT_MAXCHI2)
//            cand=null;
        return cand;
    }

}
