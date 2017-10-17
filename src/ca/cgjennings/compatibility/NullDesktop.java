package ca.cgjennings.compatibility;

import java.net.URI;

class NullDesktop extends DesktopLayer {
	@Override
	public boolean browse( URI uri ) {
		return false;
	}

	@Override
	public boolean open( URI uri ) {
		return false;
	}

	@Override
	public boolean edit( URI uri ) {
		return false;
	}

	@Override
	public boolean print( URI uri ) {
		return false;
	}

	@Override
	public boolean email( URI uri ) {
		return false;
	}

	@Override
	public boolean isSupported( int action ) {
		if( action >= DesktopLayer.ACTION_MIN && action <= DesktopLayer.ACTION_MAX ) {
			return false;
		}
		throw new IllegalArgumentException( "invalid action " + action );
	}
}
