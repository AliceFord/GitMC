# GitMC
## A fabric mod to integrate github into minecraft
This fabric mod adds some commands to your game, to allow you to back up a minecraft world on github.


### Commands:
#### gitmc
- ```/gitmc help``` - Shows a handy help screen.
- ```/gitmc login``` - Attempts to log in the user to the github api using first past tokens, then the refresh token, and then the username set with ```/gitmcset gitusername=<username>```.

#### gitmcset
- ```/gitmcset gitusername=<username>``` - Sets the github username for the program to back up worlds to.
