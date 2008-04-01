<!--
   Copyright 2002 Sun Microsystems, Inc. All Rights Reserved.
  
  Redistribution and use in source and binary forms, with or
  without modification, are permitted provided that the following
  conditions are met:
  
  - Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
  
  - Redistribution in binary form must reproduce the above
    copyright notice, this list of conditions and the following
    disclaimer in the documentation and/or other materials
    provided with the distribution.
  
  Neither the name of Sun Microsystems, Inc. or the names of
  contributors may be used to endorse or promote products derived
  from this software without specific prior written permission.
  
  This software is provided "AS IS," without a warranty of any
  kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
  WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
  EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
  DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT OF OR
  RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THIS SOFTWARE OR
  ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
  FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
  SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
  CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
  THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
  BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
  
  You acknowledge that this software is not designed, licensed or
  intended for use in the design, construction, operation or
  maintenance of any nuclear facility.
-->

<HTML>
    <HEAD> <title>Hello</title> </HEAD>
    <%@ taglib uri="http://java.sun.com/j2ee/html_basic/" prefix="faces" %>
    <body bgcolor="white">
    <img src="/guessNumber/wave.med.gif">
    <h2>Hi. My name is Duke.  I'm thinking of a number from 0 to 10.
    Can you guess it?</h2>
    <jsp:useBean id="UserNumberBean" class="guessNumber.UserNumberBean" scope="session" />
    <faces:usefaces>
    <faces:form id="helloForm" formName="helloForm" >
  	<faces:input_number id="userNo" numberStyle="NUMBER"
   				modelReference="UserNumberBean.userNumber">
	        <faces:validator 
                       className="javax.faces.validator.LongRangeValidator"/>

		     <faces:attribute 
                         name="javax.faces.validator.LongRangeValidator.MINIMUM"
                         value="0"/>

		     <faces:attribute 
                         name="javax.faces.validator.LongRangeValidator.MAXIMUM"
                         value="10"/>

         </faces:input_number> 
	 <faces:command_button id="submit" commandName="submit">
         Submit
         </faces:command_button>
         <p>
	 <faces:output_errors id="errors1" compoundId="/helloForm/userNo"/>
    </faces:form>
    </faces:usefaces>
</HTML>  
