// @(#)Complex.java   7/2003
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL) 
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer. 
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution. 
// 3. Neither the name of "DRCL" nor the names of its contributors may be used
//    to endorse or promote products derived from this software without specific
//    prior written permission. 
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// 

package drcl.util.scalar;

 /**
  * This class implements complex numbers. It provides basic operations
  * such as addition, subtraction, multiplication, division and absolute values.
  * @author Honghai Zhang
  * @version 1.0  02/04/2004
  */

public class Complex {
    private double re;
    private double im;

	/**
	  * Constructs a Complex 0.
	  */
    public Complex(){
        re=0;
        im=0;
    }
	/** 
	  * Constructs a Complex re + i * im.
	  */
    public Complex(double re, double im){
        this.re=re;
        this.im=im;
    }
	/**
	  * Constructs a Comlex equal to z.
	  */
    public Complex(Complex z){
        this.re=z.real();
        this.im=z.imag();
    }

	/** Sets the real part of the Complex number.  */
    public void setReal(double re){
        this.re=re;
    }

	/** Sets the Imaginary part of the Complex number. */
    public void setImag(double im){
        this.im=im;
    }

	/** 
	 *	Returns the absolute value (modulus) of a Complex, |z|. 
	 */
	public static double abs(Complex z)
	{
		if (z.re == 0) return Math.abs(z.im);
		else if (z.im == 0) return Math.abs(z.re);
		else return Math.sqrt(z.re * z.re + z.im * z.im);
		
	}

	/** Returns the conjugate of this current Complex. */
    public Complex conjugate(){
        Complex result = new Complex(this);
        result.setImag(-im);
        return result;
    }
	/** Returns the real part of the Complex. */
    public double real(){
        return re;
    }
	/** Returns the imaginary part of the Complex. */
    public double imag(){
        return im;
    }

	/** Returns the product of two complex number */
    public static Complex multiply (Complex y, Complex z){
        Complex result = new Complex(
        y.real() *z.real()- y.imag() *z.imag(),
        y.imag()*z.real()+y.real() *z.imag());
        return result;
    }

	/** Returns the product of a complex z with a real number x.*/
    public static Complex multiply(Complex z, double x){
        Complex result = new Complex(
        z.real() * x,
        z.imag() * x);
        return result;
    }
	/** Returns the product of a real number x with a complex z.*/
    public static Complex multiply( double x, Complex z){
		return Complex.multiply(z,x);
	}

	/**  Returns the square root of a Complex number z.
	    Its  real part is always nonegative and
		imaginary part is nonnegative 
		if it can be chosen. 
		*/
	public static Complex sqrt(Complex z)
	{
		double absz = Complex.abs(z);
		double r, i;
		r = Math.sqrt((z.re + absz) * 0.5); 
		if (z.im >= 0) { i = Math.sqrt((absz -z.re) * 0.5); }
		else { i = - Math.sqrt((absz -z.re) * 0.5); }
		return  new Complex(r,i);
	}
	/**  Returns a Complex y/z for Complex y and Complex z.
	  */

	public static Complex divide(Complex y, Complex z)
	{
		double sqrz = z.re * z.re + z.im * z.im;
		double r = y.re * z.re + y.im * z.im;
		double i = y.im * z.re - y.re * z.im;
		return new Complex(r/sqrz, i/sqrz);
	}

	/** Returns the sum of Complex y and Complex z. */
    public static Complex add(Complex y, Complex z){
        Complex result = new Complex(
        y.re + z.re, y.im + z.im);
        return result;
    }
	/** Returns the sum of Complex y and double x. */
    public static Complex add(Complex y, double x){
        Complex result = new Complex(
				y.re + x, y.im);
        return result;
    }

	/** Returns the sum of double x and Complex y. */
    public static Complex add(double x, Complex y){
        Complex result = new Complex(
				y.re + x, y.im);
        return result;
    }

	/** Returns  y -z for Complex y and Complex z. */
    public static Complex subtract(Complex y, Complex z){
        Complex result = new Complex(
				y.re - z.re, y.im - z.im);
        return result;
    }
	/** Returns y - x for Complex y and double x. */
    public static Complex subtract (Complex y, double x){
        Complex result = new Complex(
				y.re - x, y.im);
        return result;
    }
	/** Returns x - z for  double x and Complex z. */
    public static Complex subtract (double x, Complex z){
        Complex result = new Complex(
				x - z.re, -z.im);
        return result;
    }
}



