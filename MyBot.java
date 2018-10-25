
import hlt.*;

import java.util.ArrayList;
import java.util.*;

public class MyBot {

	public static Position closestCorner(Ship s,GameMap gameMap) {
		if(s.getXPos() < gameMap.getWidth() / 2 && s.getYPos() < gameMap.getHeight() / 2 )
			return new Position(1,1);
		
		if(s.getXPos() > gameMap.getWidth() / 2 && s.getYPos() > gameMap.getHeight() / 2 )
			return new Position(gameMap.getWidth() - 1,gameMap.getHeight() - 1);
		
		if(s.getXPos() < gameMap.getWidth() / 2 && s.getYPos() > gameMap.getHeight() / 2 )
			return new Position(1,gameMap.getHeight() - 1);
		
		if(s.getXPos() > gameMap.getWidth() / 2 && s.getYPos() < gameMap.getHeight() / 2 )
			return new Position(gameMap.getWidth() - 1,1);
		
		return new Position(0,0);
		
	}
	
	public static void main(final String[] args) {
		final Networking networking = new Networking();
		final GameMap gameMap = networking.initialize("FourPlay --- Bot");

		// We now have 1 full minute to analyse the initial map.
		final String initialMapIntelligence = "width: " + gameMap.getWidth() + "; height: " + gameMap.getHeight()
				+ "; players: " + gameMap.getAllPlayers().size() + "; planets: " + gameMap.getAllPlanets().size();
		Log.log(initialMapIntelligence);

		int cornerId = -1; 
		int shipCount = 0;
		boolean cornerShipStillAlive = false;
		
		final ArrayList<Move> moveList = new ArrayList<>();
		for (;;) {
			moveList.clear();
			networking.updateMap(gameMap);
			
			shipCount = gameMap.getMyPlayer().getShips().size();
			
			ArrayList<Ship> stoppedShips = new ArrayList<Ship>(); 

			for (final Ship ship1 : gameMap.getMyPlayer().getShips().values()) {
				for (final Ship ship2 : gameMap.getMyPlayer().getShips().values()) {
					if (ship1.getId() != ship2.getId()) {
						if (ship1.getDistanceTo(ship2) <= 5 && ship1.getDockingStatus() == Ship.DockingStatus.Undocked && ship2.getDockingStatus() == Ship.DockingStatus.Undocked) {
							if (!stoppedShips.contains(ship1) && !stoppedShips.contains(ship2)) {
								final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship1, ship2,
										0);
								if (newThrustMove != null) {
									moveList.add(newThrustMove);
								}
								stoppedShips.add(ship1);
							}
						}
					}
				}
			}
		
			
			ArrayList<Ship> myShips = new ArrayList<Ship>(gameMap.getMyPlayer().getShips().values());
			
			for(Ship ship : myShips) {
				cornerShipStillAlive = false;
				if(ship.getId() == cornerId) {
					cornerShipStillAlive = true;
				}
			}
			
			Collections.sort(myShips, new Comparator<Ship>() {
				public int compare(Ship s1,Ship s2) {
					if(s1.getDistanceTo(closestCorner(s1,gameMap)) > s2.getDistanceTo(closestCorner(s2,gameMap)))
						return 1;
					else if(s1.getDistanceTo(closestCorner(s1,gameMap)) < s2.getDistanceTo(closestCorner(s2,gameMap)))
						return -1;
					else return 0;
				}
			});
			
			for (final Ship ship : myShips) {
				if (ship.getDockingStatus() != Ship.DockingStatus.Undocked || stoppedShips.contains(ship)) {
					continue;
				}
				

				Set<Map.Entry<Double, Entity>> nearbyEntities = gameMap.nearbyEntitiesByDistance(ship).entrySet();

				if((cornerId == -1 || !cornerShipStillAlive)  && shipCount > 7) {
					cornerId = ship.getId();
					cornerShipStillAlive = true;
				}
				
				if(ship.getId() == cornerId) {
					
					
					final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, closestCorner(ship,gameMap), Constants.MAX_SPEED, true, Constants.MAX_NAVIGATION_CORRECTIONS, 0);
					
					
					
					if (newThrustMove != null) {
						moveList.add(newThrustMove);
					}
					
					continue;
				}
				
				for (Map.Entry<Double, Entity> entry : nearbyEntities) {
					if (entry.getValue() instanceof Planet) {
						Planet p = (Planet) entry.getValue();
						
						if(p.isOwned()) {
							if( p.getOwner() != gameMap.getMyPlayerId())
								continue;
							
							if(p.getOwner() == gameMap.getMyPlayerId() && p.isFull()) {
								continue;
							}
						}

						if (ship.canDock(p)) {
							moveList.add(new DockMove(ship, p));
							break;
						}

						final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, p,
								Constants.MAX_SPEED);
						if (newThrustMove != null) {
							moveList.add(newThrustMove);
						}

						break;
					} else if(entry.getValue() instanceof Ship && entry.getValue().getOwner() != gameMap.getMyPlayerId()) {
						Ship enemyShip = (Ship) entry.getValue();

						final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, enemyShip,
								Constants.MAX_SPEED);
						if (newThrustMove != null) {
							moveList.add(newThrustMove);
						}

						break;

					}
				}

			}
			Networking.sendMoves(moveList);
		}
	}
}
 
