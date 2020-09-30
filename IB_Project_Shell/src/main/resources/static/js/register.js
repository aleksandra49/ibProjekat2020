$(document).ready(function(){
	
	$('#registrationSubmit').on('click', function(){
		
			var email =  $('#korisnickoImeInput').val().trim();
			var password = $('#lozinkaInput').val().trim();
			console.log(email+" "+password);
			if(email=="" || password==""){
				alert("All fields must be filled.")
				return;
			}
			
			var data = {
					'email':email,
					'password':password
			}
			console.log(data);
			
			$.ajax("users/register", {
			   type: "POST",
			   data: data,
			   statusCode: {
			      200: function (response) {
			    	  alert('Uspesno ste se registrovali.');
			      },
			      400: function (response) {
			         alert('Korisnik vec postoji.');
			      },
			      404: function (response) {
			         alert('');
			      }
			   }, 
			});
			

			
		

	});
	
});