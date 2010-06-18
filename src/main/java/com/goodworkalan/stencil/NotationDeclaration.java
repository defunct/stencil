package com.goodworkalan.stencil;

/**
 * A notation declaration in an XML document. Used by {@link InOrderDocument} to 
 * capture notation declarations.
 *
 * @author Alan Gutierrez
 */
class NotationDeclaration {
	/** The name. */
	public final String name;
	
	/** The public id. */
	public final String publicId;
	
	/** The system id. */
	public final String systemId;

	/**
	 * Create a notation declaration with the given name, public id, and system
	 * id.
	 * 
	 * @param name
	 *            The name.
	 * @param publicId
	 *            The public id.
	 * @param systemId
	 *            The system id.
	 */
	public NotationDeclaration(String name, String publicId, String systemId) {
		this.name = name;
		this.publicId = publicId;
		this.systemId = systemId;
	}
}
