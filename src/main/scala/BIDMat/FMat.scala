/* Copyright (c) 2012, Regents of the University of California                     */
/* All rights reserved.                                                            */

/* Redistribution and use in source and binary forms, with or without              */
/* modification, are permitted provided that the following conditions are met:     */
/*     * Redistributions of source code must retain the above copyright            */
/*       notice, this list of conditions and the following disclaimer.             */
/*     * Redistributions in binary form must reproduce the above copyright         */
/*       notice, this list of conditions and the following disclaimer in the       */
/*       documentation and/or other materials provided with the distribution.      */
/*     * Neither the name of the <organization> nor the                            */
/*       names of its contributors may be used to endorse or promote products      */
/*       derived from this software without specific prior written permission.     */

/* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND */
/* ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED   */
/* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          */
/* DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY              */
/* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES      */
/* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;    */
/* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND     */
/* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT      */
/* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS   */
/* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                    */

package BIDMat
import edu.berkeley.bid.CBLAS._
import edu.berkeley.bid.LAPACK._

case class FMat(nr:Int, nc:Int, data0:Array[Float]) extends DenseMat[Float](nr, nc, data0) {

  def size() = length;
  
  def tryForFMat(m:Mat, s:String):FMat = 
  	m match {
  	case mm:FMat => mm
  	case _ => throw new RuntimeException("wrong type for operator "+s+" arg "+m)
  }
    
  def tryForOutFMat(out:Mat):FMat = 
  	if (out == null) {
  		null
  	} else {
  		out match {
  		case outmat:FMat => outmat
  		case _ => throw new RuntimeException("wrong type for LHS matrix "+out)
  		}
  	}
  
  override def t:FMat = FMat(gt(null))
  
  def i:CMat = CMat.imag(this)
  
  def horzcat(b: FMat) = FMat(ghorzcat(b))
  
  def vertcat(b: FMat) = FMat(gvertcat(b))
  
  def find3:(IMat, IMat, FMat) = { val (ii, jj, vv) = gfind3 ; (IMat(ii), IMat(jj), FMat(vv)) }
  
  override def apply(a:IMat):FMat = FMat(gapply(a))
  
  override def apply(a:IMat, b:IMat):FMat = FMat(gapply(a, b))	
  
  override def apply(a:IMat, b:Int):FMat = FMat(gapply(a, b))	
  
  override def apply(a:Int, b:IMat):FMat = FMat(gapply(a, b))
  
  def ffMatOp(b: Mat, f:(Float, Float) => Float, out:Mat):FMat = 
    b match {
      case bb:FMat => FMat(ggMatOp(bb, f, tryForOutFMat(out)))
      case _ => throw new RuntimeException("unsupported operation "+f+" on "+this+" and "+b)	
    }
  
  def ffMatOpv(b: Mat, f:(Array[Float],Int,Int,Array[Float],Int,Int,Array[Float],Int,Int,Int) => Float, out:Mat) = 
    b match {
      case bb:FMat => FMat(ggMatOpv(bb, f, tryForOutFMat(out)))
      case _ => throw new RuntimeException("unsupported operation "+f+" on "+this+" and "+b)	
    }
  
  def ffMatOpScalar(b: Float, f:(Float, Float) => Float, out:Mat):FMat = FMat(ggMatOpScalar(b, f, tryForOutFMat(out)))
  
  def ffMatOpScalarv(b: Float, f:(Array[Float],Int,Int,Array[Float],Int,Int,Array[Float],Int,Int,Int) => Float, out:Mat) = 
    FMat(ggMatOpScalarv(b, f, tryForOutFMat(out)))
  
  def ffReduceOp(n:Int, f1:(Float) => Float, f2:(Float, Float) => Float, out:Mat) = 
    FMat(ggReduceOp(n, f1, f2, tryForOutFMat(out)))
  
  def ffReduceOpv(n:Int, f:(Array[Float],Int,Int,Array[Float],Int,Int,Array[Float],Int,Int,Int) => Float, out:Mat) = 
    FMat(ggReduceOpv(n, f, tryForOutFMat(out)))
  
  def ffReduceAll(n:Int, f1:(Float) => Float, f2:(Float, Float) => Float, out:Mat) = 
    FMat(ggReduceAll(n, f1, f2, tryForOutFMat(out)))
  
  def ffReduceAllv(n:Int, f:(Array[Float],Int,Int,Array[Float],Int,Int,Array[Float],Int,Int,Int) => Float, out:Mat) = 
    FMat(ggReduceAllv(n, f, tryForOutFMat(out)))
  
  override def printOne(i:Int):String = {
    val v = data(i)
    if (v % 1 == 0 && math.abs(v) < 1e10) {	      
      "%d" format v.intValue
    } else {
      "%.5g" format v
    }
  }
  
  override def copy = {
  	val out = FMat(nrows, ncols)
  	System.arraycopy(data, 0, out.data, 0, length)
  	out
  }
  
  override def copy(a:Mat) = {
  	a match {
  	  case out:FMat => System.arraycopy(data, 0, out.data, 0, length)
  	}
  	a
  }
  
  override def zeros(nr:Int, nc:Int) = {
  	FMat(nr, nc)
  }
  
  override def ones(nr:Int, nc:Int) = {
  	val out = FMat(nr, nc)
  	var i = 0
  	while (i < out.length) {
  	  out(i) = 1
  	  i += 1
  	}
  	out
  }
   
  override def clearUpper(off:Int) = setUpper(0, off)
  override def clearUpper = setUpper(0, 0)
  
  override def clearLower(off:Int) = setLower(0, off)
  override def clearLower = setLower(0, 0)

  
  def fDMult(a:Mat, outmat:FMat):FMat = { 
    import edu.berkeley.bid.SPBLAS._
    a match {
      case aa:FMat => {
	if (ncols == a.nrows) {
	  val out = FMat.newOrCheckFMat(nrows, a.ncols, outmat)
	  Mat.nflops += 2L * length * a.ncols
	  if (Mat.noMKL) {
	  	if (outmat != null) out.clear
	  	var i = 0
	  	while (i < a.ncols) {
	  		var j = 0
	  		while (j < a.nrows) {
	  			var k = 0
	  			val dval = aa.data(j + i*ncols)
	  			while (k < nrows) {
	  				out.data(k+i*nrows) += data(k+j*nrows)*dval
	  				k += 1
	  			}
	  			j += 1
	  		}
	  		i += 1									
	  	}
	  } else if (nrows == 1) {
	    sgemv(ORDER.ColMajor, TRANSPOSE.Trans, a.nrows, a.ncols, 1.0f, aa.data, a.nrows, data, 1, 0, out.data, 1)
	  } else if (a.ncols == 1) {
	    sgemv(ORDER.ColMajor, TRANSPOSE.NoTrans, nrows, ncols, 1.0f, data, nrows, aa.data, 1, 0, out.data, 1)
	  } else {
	    sgemm(ORDER.ColMajor, TRANSPOSE.NoTrans, TRANSPOSE.NoTrans,
		  nrows, a.ncols, ncols, 1.0f, data, nrows, aa.data, a.nrows, 0, out.data, nrows)
	  }
	  out
	} else if (ncols == 1 && nrows == 1){
	  val out = FMat.newOrCheckFMat(a.nrows, a.ncols, outmat)
	  Mat.nflops += aa.length
	  var i = 0
	  val dvar = data(0)
	  while (i < aa.length) {
	    out.data(i) = dvar * aa.data(i)
	    i += 1
	  }			    
	  out			  
	} else if (a.ncols == 1 && a.nrows == 1){
	  val out = FMat.newOrCheckFMat(nrows, ncols, outmat)
	  Mat.nflops += length
	  var i = 0
	  val dvar = aa.data(0)
	  while (i < length) {
	    out.data(i) = dvar * data(i)
	    i += 1
	  }			    
	  out			  
	}	else throw new RuntimeException("dimensions mismatch")
      }
      case ss:SMat => {
	if (ncols != a.nrows) {
	  throw new RuntimeException("dimensions mismatch")
	} else {
	  val out = FMat.newOrCheckFMat(nrows, a.ncols, outmat)
	  Mat.nflops += 2 * nrows.toLong * ss.nnz
	  val ioff = Mat.ioneBased;
	  val nr = ss.nrows
	  val nc = ss.ncols
	  val kk = ncols
	  var jc0:Array[Int] = null
	  var ir0:Array[Int] = null
	  if (ioff == 0) {
	    jc0 = SparseMat.incInds(ss.jc)
	    ir0 = SparseMat.incInds(ss.ir)
	  } else {
	    jc0 = ss.jc
	    ir0 = ss.ir
	  }	 
	  if (nrows == 1 && !Mat.noMKL) {
	    scscmv("T", nr, nc, 1.0f, "GLNF", ss.data, ir0, jc0, data, 0f, out.data)
	    out
	  } else {
	    if (outmat != null) out.clear
	    if (nrows < 20 || Mat.noMKL) {
	      var i = 0
	      while (i < a.ncols) {
		var j = ss.jc(i) - ioff
		while (j < ss.jc(i+1)-ioff) {
		  val dval = ss.data(j)
		  val ival = ss.ir(j) - ioff
		  var k = 0
		  while (k < nrows) {
		    out.data(k+i*nrows) += data(k+ival*nrows)*dval
		    k += 1
		  }
		  j += 1
		}
		i += 1
	      }
	    } else {
	      smcscm(nrows, ss.ncols, data, nrows, ss.data, ss.ir, ss.jc, out.data, nrows)
//              scsrmm("N", ss.ncols, nrows, ncols, 1.0f, "GLNF", ss.data, ss.ir, ss.jc, data, ncols, 0f, out.data, out.ncols)
	    }
	  }
	  out
	}
      }
      case _ => throw new RuntimeException("unsupported arg")	
    }
  }
  /*
  * Column-based (Streaming) multiply
  */
  
  def DMult(a:Mat):FMat = 
    a match {
      case aa:FMat => {
	if (ncols == a.nrows) {
	  val out = FMat(nrows, a.ncols) // Needs to be cleared
	  for (i <- 0 until a.ncols)
	    for (j <- 0 until a.nrows) {
	      var k = 0
	      val dval = aa.data(j + i*ncols)
	      while (k < nrows) {
		out.data(k+i*nrows) += data(k+j*nrows)*dval
		k += 1
	      }
	    }
	  out
	} else throw new RuntimeException("dimensions mismatch")
      }
      case _ => throw new RuntimeException("argument must be dense")
    }
  
  /*
   * Very slow, row-and-column multiply
   */
  
  def sDMult(a:Mat):FMat = 
    a match {
      case aa:FMat => {
	if (ncols == a.nrows) {
	  val out = FMat(nrows, a.ncols)
	  for (i <- 0 until a.ncols)
	    for (j <- 0 until nrows) {
	      var k = 0
	      var sum = 0f
	      while (k < ncols) {
		sum += data(j+k*nrows) * aa.data(k+i*a.nrows)
		k += 1
	      }
	      out.data(j + i*out.nrows) = sum
	    }
	  out
	} else throw new RuntimeException("dimensions mismatch")
      }
      case _ => throw new RuntimeException("argument must be dense")
    }
  
  def dot (a : Mat):Float = 
    a match { 
      case b:FMat => 
        if (math.min(nrows, ncols) != 1 || math.min(b.nrows,b.ncols) != 1 || length != b.length) {
          throw new RuntimeException("vector dims not compatible")
        } else {
          Mat.nflops += 2 * length
          var v = 0f
          var i = 0
          while (i < length){
	        v += data(i)*b.data(i)
	        i += 1
          }
          v
        }
      case _ => throw new RuntimeException("unsupported arg to dot "+a)
    };

  def solvel(a0:Mat):FMat = 
    a0 match {
      case a:FMat => { 
        Mat.nflops += 2L*a.nrows*a.nrows*a.nrows/3 + 2L*nrows*a.nrows*a.nrows
        if (a.nrows != a.ncols || ncols != a.nrows) {
          throw new RuntimeException("solve needs a square matrix")
        } else {
          val out = FMat(nrows, ncols)
          val tmp = new Array[Float](ncols*ncols)
          System.arraycopy(a.data, 0, tmp, 0, a.length)
          System.arraycopy(data, 0, out.data, 0, length)
          val ipiv = new Array[Int](ncols)
          sgetrf(ORDER.RowMajor, ncols, ncols, tmp, ncols, ipiv)
          sgetrs(ORDER.RowMajor, "N", ncols, nrows, tmp, ncols, ipiv, out.data, nrows)
          out
        }
      }
      case _ => throw new RuntimeException("unsupported arg to / "+a0)
    }
  
  def solver(a0:Mat):FMat = 
    a0 match {
      case a:FMat => { 
        Mat.nflops += 2L*nrows*nrows*nrows/3 + 2L*nrows*nrows*a.ncols
        if (nrows != ncols || ncols != a.nrows) {
          throw new RuntimeException("solve needs a square matrix")
        } else {
          val out = FMat(a.nrows, a.ncols)
          val tmp = new Array[Float](ncols*ncols)
          System.arraycopy(data, 0, tmp, 0, length)
          System.arraycopy(a.data, 0, out.data, 0, a.length)
          val ipiv = new Array[Int](ncols)
          sgetrf(ORDER.ColMajor, ncols, ncols, tmp, ncols, ipiv)
          sgetrs(ORDER.ColMajor, "N", ncols, a.ncols, tmp, nrows, ipiv, out.data, nrows)
          out
        }
      }
      case _ => throw new RuntimeException("unsupported arg to \\ "+a0)
    }
  
  def inv:FMat = {
    import edu.berkeley.bid.LAPACK._
    if (nrows != ncols) {
      throw new RuntimeException("inv method needs a square matrix")
    } else {
      val out = FMat(nrows, ncols)
      System.arraycopy(data, 0, out.data, 0, length)
      val ipiv = new Array[Int](nrows)
      sgetrf(ORDER.ColMajor, nrows, ncols, out.data, nrows, ipiv)
      sgetri(ORDER.ColMajor, nrows, out.data, nrows, ipiv)
      out
    }
  }
  
  def clear = {
    var i = 0
    while (i < length) {
      data(i) = 0
      i += 1
    }
  }

  import MatFunctions.toFMat
  override def + (b : Mat) = ffMatOpv(toFMat(b), DenseMat.vecAdd _, null)
  override def - (b : Mat) = ffMatOpv(toFMat(b), DenseMat.vecSub _, null)
  override def * (b : Mat) = fDMult(toFMat(b), null)
  def * (b : SMat) = fDMult(b, null)
  override def / (b : Mat) = solvel(toFMat(b))
  override def \\ (b : Mat) = solver(toFMat(b))
  override def *@ (b : Mat) = ffMatOpv(toFMat(b), DenseMat.vecMul _, null)
  override def /@ (b : Mat) = ffMatOpv(toFMat(b), FMat.fVecDiv _, null)
  
  def * (b : Float) = fDMult(FMat.felem(b), null)
  def + (b : Float) = ffMatOpScalarv(b, DenseMat.vecAdd _, null)
  def - (b : Float) = ffMatOpScalarv(b, DenseMat.vecSub _, null)
  def *@ (b : Float) = ffMatOpScalarv(b, DenseMat.vecMul _, null)
  def /@ (b : Float) = ffMatOpScalarv(b, FMat.fVecDiv _, null)
  
  def * (b : Int) = fDMult(FMat.felem(b), null)
  def + (b : Int) = ffMatOpScalarv(b, DenseMat.vecAdd _, null)
  def - (b : Int) = ffMatOpScalarv(b, DenseMat.vecSub _, null)
  def *@ (b : Int) = ffMatOpScalarv(b, DenseMat.vecMul _, null)
  def /@ (b : Int) = ffMatOpScalarv(b, FMat.fVecDiv _, null)
  
  def * (b : Double) = fDMult(FMat.felem(b.asInstanceOf[Float]), null)
  def + (b : Double) = ffMatOpScalarv(b.asInstanceOf[Float], DenseMat.vecAdd _, null)
  def - (b : Double) = ffMatOpScalarv(b.asInstanceOf[Float], DenseMat.vecSub _, null)
  def *@ (b : Double) = ffMatOpScalarv(b.asInstanceOf[Float], DenseMat.vecMul _, null)
  def /@ (b : Double) = ffMatOpScalarv(b.asInstanceOf[Float], FMat.fVecDiv _, null)
  
  override def > (b : Mat) = ffMatOp(toFMat(b), (x:Float, y:Float) => if (x > y) 1f else 0f, null)
  override def < (b : Mat) = ffMatOp(toFMat(b), (x:Float, y:Float) => if (x < y) 1f else 0f, null)
  override def == (b : Mat) = ffMatOp(toFMat(b), (x:Float, y:Float) => if (x == y) 1f else 0f, null)
  override def === (b : Mat) = ffMatOp(toFMat(b), (x:Float, y:Float) => if (x == y) 1f else 0f, null)
  override def >= (b : Mat) = ffMatOp(toFMat(b), (x:Float, y:Float) => if (x >= y) 1f else 0f, null)
  override def <= (b : Mat) = ffMatOp(toFMat(b), (x:Float, y:Float) => if (x <= y) 1f else 0f, null)
  override def != (b : Mat) = ffMatOp(toFMat(b), (x:Float, y:Float) => if (x != y) 1f else 0f, null)

  def > (b : Float) = ffMatOpScalar(b, (x:Float, y:Float) => if (x > y) 1f else 0f, null)
  def < (b : Float) = ffMatOpScalar(b, (x:Float, y:Float) => if (x < y) 1f else 0f, null)
  def == (b : Float) = ffMatOpScalar(b, (x:Float, y:Float) => if (x == y) 1f else 0f, null)
  def === (b : Float) = ffMatOpScalar(b, (x:Float, y:Float) => if (x == y) 1f else 0f, null)
  def >= (b : Float) = ffMatOpScalar(b, (x:Float, y:Float) => if (x >= y) 1f else 0f, null)
  def <= (b : Float) = ffMatOpScalar(b, (x:Float, y:Float) => if (x <= y) 1f else 0f, null)
  def != (b : Float) = ffMatOpScalar(b, (x:Float, y:Float) => if (x != y) 1f else 0f, null) 
  
  def > (b : Int) = ffMatOpScalar(b, (x:Float, y:Float) => if (x > y) 1f else 0f, null)
  def < (b : Int) = ffMatOpScalar(b, (x:Float, y:Float) => if (x < y) 1f else 0f, null)
  def == (b : Int) = ffMatOpScalar(b, (x:Float, y:Float) => if (x == y) 1f else 0f, null)
  def === (b : Int) = ffMatOpScalar(b, (x:Float, y:Float) => if (x == y) 1f else 0f, null)
  def >= (b : Int) = ffMatOpScalar(b, (x:Float, y:Float) => if (x >= y) 1f else 0f, null)
  def <= (b : Int) = ffMatOpScalar(b, (x:Float, y:Float) => if (x <= y) 1f else 0f, null)
  def != (b : Int) = ffMatOpScalar(b, (x:Float, y:Float) => if (x != y) 1f else 0f, null) 
  
  def \ (b: FMat) = horzcat(b)
  override def \ (b: Mat) = b match {
    case fb:FMat => horzcat(fb)
    case db:DMat => MatFunctions.fMat2DMat(this).horzcat(db)
  }
  def \ (b: Float) = horzcat(FMat.felem(b))
  
  def on (b: FMat) = vertcat(b)
  override def on (b: Mat) = b match {
    case fb:FMat => vertcat(fb)
    case db:DMat => MatFunctions.fMat2DMat(this).vertcat(db)
  }
  def on (b: Float) = vertcat(FMat.felem(b))
  
  override def ~ (b: Mat):Pair = 
    b match {
    case db:FMat => new FPair(this, db)
    case sb:SMat => new SPair(this, sb)
    case _ => throw new RuntimeException("mismatched types for operator ~")
  }
}

class FPair (val omat:Mat, val mat:FMat) extends Pair {
  
  override def t:FMat = FMat(mat.gt(mat.tryForOutFMat(omat)))
  
  import MatFunctions.toFMat
  override def * (b : Mat) = mat.fDMult(toFMat(b), mat.tryForOutFMat(omat))  
  override def + (b : Mat) = mat.ffMatOpv(toFMat(b), DenseMat.vecAdd _, omat)
  override def - (b : Mat) = mat.ffMatOpv(toFMat(b), DenseMat.vecSub _, omat)
  override def *@ (b : Mat) = mat.ffMatOpv(toFMat(b), DenseMat.vecMul _, omat)
  override def /@ (b : Mat) = mat.ffMatOpv(toFMat(b), FMat.fVecDiv _, omat)  
  override def ^ (b : Mat) = mat.ffMatOp(toFMat(b), (x:Float, y:Float) => math.pow(x,y).toFloat, null)  

  override def > (b : Mat) = mat.ffMatOp(toFMat(b), (x:Float, y:Float) => if (x > y) 1.0f else 0.0f, omat)
  override def < (b : Mat) = mat.ffMatOp(toFMat(b), (x:Float, y:Float) => if (x < y) 1.0f else 0.0f, omat)
  override def == (b : Mat) = mat.ffMatOp(toFMat(b), (x:Float, y:Float) => if (x == y) 1.0f else 0.0f, omat)
  override def === (b : Mat) = mat.ffMatOp(toFMat(b), (x:Float, y:Float) => if (x == y) 1.0f else 0.0f, omat)
  override def >= (b : Mat) = mat.ffMatOp(toFMat(b), (x:Float, y:Float) => if (x >= y) 1.0f else 0.0f, omat)
  override def <= (b : Mat) = mat.ffMatOp(toFMat(b), (x:Float, y:Float) => if (x <= y) 1.0f else 0.0f, omat)
  override def != (b : Mat) = mat.ffMatOp(toFMat(b), (x:Float, y:Float) => if (x != y) 1.0f else 0.0f, omat) 
  
  def * (b : Float) = mat.fDMult(FMat.felem(b), mat.tryForOutFMat(omat))
  def + (b : Float) = mat.ffMatOpScalarv(b, DenseMat.vecAdd _, null)
  def - (b : Float) = mat.ffMatOpScalarv(b, DenseMat.vecSub _, null)
  def *@ (b : Float) = mat.ffMatOpScalarv(b, DenseMat.vecMul _, null)
  def /@ (b : Float) = mat.ffMatOpScalarv(b, FMat.fVecDiv _, null)
  def ^ (b : Float) = mat.ffMatOpScalar(b, (x:Float, y:Float) => math.pow(x,y).toFloat, omat)

  def > (b : Float) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x > y) 1.0f else 0.0f, omat)
  def < (b : Float) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x < y) 1.0f else 0.0f, omat)
  def == (b : Float) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x == y) 1.0f else 0.0f, omat)
  def >= (b : Float) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x >= y) 1.0f else 0.0f, omat)
  def <= (b : Float) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x <= y) 1.0f else 0.0f, omat)
  def != (b : Float) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x != y) 1.0f else 0.0f, omat)  
    
  def * (b : Int) = mat.fDMult(FMat.felem(b), mat.tryForOutFMat(omat))
  def + (b : Int) = mat.ffMatOpScalarv(b, DenseMat.vecAdd _, null)
  def - (b : Int) = mat.ffMatOpScalarv(b, DenseMat.vecSub _, null)
  def *@ (b : Int) = mat.ffMatOpScalarv(b, DenseMat.vecMul _, null)
  def /@ (b : Int) = mat.ffMatOpScalarv(b, FMat.fVecDiv _, null)
  def ^ (b : Int) = mat.ffMatOpScalar(b, (x:Float, y:Float) => math.pow(x,y).toFloat, omat)

  def > (b : Int) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x > y) 1.0f else 0.0f, omat)
  def < (b : Int) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x < y) 1.0f else 0.0f, omat)
  def == (b : Int) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x == y) 1.0f else 0.0f, omat)
  def >= (b : Int) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x >= y) 1.0f else 0.0f, omat)
  def <= (b : Int) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x <= y) 1.0f else 0.0f, omat)
  def != (b : Int) = mat.ffMatOpScalar(b, (x:Float, y:Float) => if (x != y) 1.0f else 0.0f, omat) 
}

object FMat {
  
  def fVecDiv(a:Array[Float], a0:Int, ainc:Int, b:Array[Float], b0:Int, binc:Int, c:Array[Float], c0:Int, cinc:Int, n:Int):Float = {
    var ai = a0; var bi = b0; var ci = c0; var cend = c0 + n
    while (ci < cend) {
      c(ci) = a(ai) / b(bi);  ai += ainc; bi += binc;  ci += cinc
    }
    0f
  }
  
  def apply(nr:Int, nc:Int) = new FMat(nr, nc, new Array[Float](nr*nc))
  
  def apply(a:DenseMat[Float]):FMat = new FMat(a.nrows, a.ncols, a.data) 

  def apply(x:Mat):FMat = {
    val out = FMat(x.nrows, x.ncols)
    x match {
      case dd:DMat => Mat.copyToFloatArray(dd.data, 0, out.data, 0, dd.length)
      case ff:FMat => System.arraycopy(ff.data, 0, out.data, 0, ff.length)
      case ii:IMat => Mat.copyToFloatArray(ii.data, 0, out.data, 0, ii.length)
      case ss:SMat => FMat(ss.full)
      case gg:GMat => gg.toFMat
      case _ => throw new RuntimeException("Unsupported source type")
    }
    out
  }

  def felem(x:Float) = {
    val out = FMat(1,1)
    out.data(0) = x
    out
  }
  
  def newOrCheckFMat(nr:Int, nc:Int, outmat:FMat):FMat = {
    if (outmat == null) {
      FMat(nr, nc)
    } else {
      if (outmat.nrows != nr || outmat.ncols != nc) {
        throw new RuntimeException("dimensions mismatch")
      } else {
      	outmat
      }
    }
  }
}






