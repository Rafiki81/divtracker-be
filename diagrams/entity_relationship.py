"""
DivTracker Entity-Relationship Diagram
Database schema visualization using Graphviz DOT language
"""

import os
import subprocess

# Create a simple ER diagram using custom nodes
graph_attr = {
    "fontsize": "16",
    "bgcolor": "white",
    "pad": "0.5",
    "splines": "ortho",
    "nodesep": "1",
    "ranksep": "1.2",
}

node_attr = {
    "fontsize": "12",
    "fontname": "Helvetica",
}

edge_attr = {
    "fontsize": "10",
    "fontname": "Helvetica",
}

# Since diagrams library doesn't have ER diagram primitives,
# we'll create a text-based ER diagram using graphviz directly
er_diagram = """
digraph ER {
    graph [
        rankdir=TB,
        bgcolor=white,
        fontname="Helvetica",
        fontsize=14,
        pad=0.5,
        nodesep=0.8,
        ranksep=1.0,
        splines=ortho
    ];
    
    node [
        shape=none,
        fontname="Helvetica",
        fontsize=11
    ];
    
    edge [
        fontname="Helvetica",
        fontsize=9,
        color="#333333"
    ];

    // Users Table
    users [label=<
        <TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="8" BGCOLOR="#E3F2FD">
            <TR><TD COLSPAN="3" BGCOLOR="#1976D2"><FONT COLOR="white"><B>users</B></FONT></TD></TR>
            <TR><TD ALIGN="LEFT">🔑</TD><TD ALIGN="LEFT"><B>id</B></TD><TD ALIGN="LEFT">UUID</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">email</TD><TD ALIGN="LEFT">VARCHAR(255) UNIQUE</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">password</TD><TD ALIGN="LEFT">VARCHAR(255)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">first_name</TD><TD ALIGN="LEFT">VARCHAR(255)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">last_name</TD><TD ALIGN="LEFT">VARCHAR(255)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">provider</TD><TD ALIGN="LEFT">ENUM (LOCAL, GOOGLE)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">provider_id</TD><TD ALIGN="LEFT">VARCHAR(255)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">role</TD><TD ALIGN="LEFT">ENUM (USER, ADMIN)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">enabled</TD><TD ALIGN="LEFT">BOOLEAN</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">created_at</TD><TD ALIGN="LEFT">TIMESTAMP</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">updated_at</TD><TD ALIGN="LEFT">TIMESTAMP</TD></TR>
        </TABLE>
    >];

    // Watchlist Items Table
    watchlist_items [label=<
        <TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="8" BGCOLOR="#E8F5E9">
            <TR><TD COLSPAN="3" BGCOLOR="#388E3C"><FONT COLOR="white"><B>watchlist_items</B></FONT></TD></TR>
            <TR><TD ALIGN="LEFT">🔑</TD><TD ALIGN="LEFT"><B>id</B></TD><TD ALIGN="LEFT">UUID</TD></TR>
            <TR><TD ALIGN="LEFT">🔗</TD><TD ALIGN="LEFT"><B>user_id</B></TD><TD ALIGN="LEFT">UUID FK</TD></TR>
            <TR><TD ALIGN="LEFT">🔗</TD><TD ALIGN="LEFT"><B>ticker</B></TD><TD ALIGN="LEFT">VARCHAR(12)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">exchange</TD><TD ALIGN="LEFT">VARCHAR(50)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">target_price</TD><TD ALIGN="LEFT">DECIMAL(19,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">target_pfcf</TD><TD ALIGN="LEFT">DECIMAL(19,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">notify_when_below_price</TD><TD ALIGN="LEFT">BOOLEAN</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">notes</TD><TD ALIGN="LEFT">VARCHAR(500)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">estimated_fcf_growth_rate</TD><TD ALIGN="LEFT">DECIMAL(5,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">investment_horizon_years</TD><TD ALIGN="LEFT">INTEGER</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">discount_rate</TD><TD ALIGN="LEFT">DECIMAL(5,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">created_at</TD><TD ALIGN="LEFT">TIMESTAMP</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">updated_at</TD><TD ALIGN="LEFT">TIMESTAMP</TD></TR>
        </TABLE>
    >];

    // User FCM Tokens Table
    user_fcm_tokens [label=<
        <TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="8" BGCOLOR="#FFF3E0">
            <TR><TD COLSPAN="3" BGCOLOR="#F57C00"><FONT COLOR="white"><B>user_fcm_tokens</B></FONT></TD></TR>
            <TR><TD ALIGN="LEFT">🔑</TD><TD ALIGN="LEFT"><B>id</B></TD><TD ALIGN="LEFT">UUID</TD></TR>
            <TR><TD ALIGN="LEFT">🔗</TD><TD ALIGN="LEFT"><B>user_id</B></TD><TD ALIGN="LEFT">UUID FK</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">fcm_token</TD><TD ALIGN="LEFT">VARCHAR(500)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">device_id</TD><TD ALIGN="LEFT">VARCHAR(255)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">device_name</TD><TD ALIGN="LEFT">VARCHAR(255)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">platform</TD><TD ALIGN="LEFT">ENUM (ANDROID, IOS, WEB)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">is_active</TD><TD ALIGN="LEFT">BOOLEAN</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">created_at</TD><TD ALIGN="LEFT">TIMESTAMP</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">updated_at</TD><TD ALIGN="LEFT">TIMESTAMP</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">last_used_at</TD><TD ALIGN="LEFT">TIMESTAMP</TD></TR>
        </TABLE>
    >];

    // Instrument Fundamentals Table
    instrument_fundamentals [label=<
        <TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="8" BGCOLOR="#F3E5F5">
            <TR><TD COLSPAN="3" BGCOLOR="#7B1FA2"><FONT COLOR="white"><B>instrument_fundamentals</B></FONT></TD></TR>
            <TR><TD ALIGN="LEFT">🔑</TD><TD ALIGN="LEFT"><B>ticker</B></TD><TD ALIGN="LEFT">VARCHAR(12)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">company_name</TD><TD ALIGN="LEFT">VARCHAR(255)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">currency</TD><TD ALIGN="LEFT">VARCHAR(10)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">sector</TD><TD ALIGN="LEFT">VARCHAR(100)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">current_price</TD><TD ALIGN="LEFT">DECIMAL(19,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">daily_change_percent</TD><TD ALIGN="LEFT">DECIMAL(10,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">market_capitalization</TD><TD ALIGN="LEFT">DECIMAL(19,2)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">week_high_52</TD><TD ALIGN="LEFT">DECIMAL(19,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">week_low_52</TD><TD ALIGN="LEFT">DECIMAL(19,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">pe_annual</TD><TD ALIGN="LEFT">DECIMAL(19,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">beta</TD><TD ALIGN="LEFT">DECIMAL(10,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">fcf_annual</TD><TD ALIGN="LEFT">DECIMAL(19,2)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">fcf_per_share_annual</TD><TD ALIGN="LEFT">DECIMAL(19,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">dividend_yield</TD><TD ALIGN="LEFT">DECIMAL(10,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">data_quality</TD><TD ALIGN="LEFT">ENUM</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">source</TD><TD ALIGN="LEFT">ENUM</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">last_updated_at</TD><TD ALIGN="LEFT">TIMESTAMP</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">created_at</TD><TD ALIGN="LEFT">TIMESTAMP</TD></TR>
        </TABLE>
    >];

    // Market Price Ticks Table
    market_price_ticks [label=<
        <TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="8" BGCOLOR="#FFEBEE">
            <TR><TD COLSPAN="3" BGCOLOR="#C62828"><FONT COLOR="white"><B>market_price_ticks</B></FONT></TD></TR>
            <TR><TD ALIGN="LEFT">🔑</TD><TD ALIGN="LEFT"><B>id</B></TD><TD ALIGN="LEFT">UUID</TD></TR>
            <TR><TD ALIGN="LEFT">🔗</TD><TD ALIGN="LEFT"><B>ticker</B></TD><TD ALIGN="LEFT">VARCHAR(12)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">price</TD><TD ALIGN="LEFT">DECIMAL(19,6)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">volume</TD><TD ALIGN="LEFT">DECIMAL(19,4)</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">trade_timestamp</TD><TD ALIGN="LEFT">TIMESTAMP</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">received_at</TD><TD ALIGN="LEFT">TIMESTAMP</TD></TR>
            <TR><TD ALIGN="LEFT"></TD><TD ALIGN="LEFT">source</TD><TD ALIGN="LEFT">VARCHAR(32)</TD></TR>
        </TABLE>
    >];

    // Relationships
    users -> watchlist_items [
        label="1:N\\nhas many",
        headlabel="*",
        taillabel="1",
        dir=both,
        arrowhead=crow,
        arrowtail=none
    ];
    
    users -> user_fcm_tokens [
        label="1:N\\nhas many",
        headlabel="*",
        taillabel="1",
        dir=both,
        arrowhead=crow,
        arrowtail=none
    ];
    
    watchlist_items -> instrument_fundamentals [
        label="N:1\\nreferences",
        style=dashed,
        headlabel="1",
        taillabel="*",
        dir=both,
        arrowhead=none,
        arrowtail=crow
    ];
    
    market_price_ticks -> instrument_fundamentals [
        label="N:1\\nreferences",
        style=dashed,
        headlabel="1",
        taillabel="*",
        dir=both,
        arrowhead=none,
        arrowtail=crow
    ];

    // Legend
    legend [label=<
        <TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="6" BGCOLOR="#FAFAFA">
            <TR><TD COLSPAN="2" BGCOLOR="#424242"><FONT COLOR="white"><B>Legend</B></FONT></TD></TR>
            <TR><TD ALIGN="LEFT">🔑</TD><TD ALIGN="LEFT">Primary Key</TD></TR>
            <TR><TD ALIGN="LEFT">🔗</TD><TD ALIGN="LEFT">Foreign Key / Index</TD></TR>
            <TR><TD ALIGN="LEFT">───</TD><TD ALIGN="LEFT">Direct Relationship</TD></TR>
            <TR><TD ALIGN="LEFT">- - -</TD><TD ALIGN="LEFT">Logical Reference (by ticker)</TD></TR>
        </TABLE>
    >];

    // Position legend
    { rank=sink; legend; }
}
"""

def generate():
    """Generate the ER diagram."""
    output_path = os.path.join(os.path.dirname(__file__), "entity_relationship")
    
    # Write the DOT file
    dot_path = output_path + ".dot"
    with open(dot_path, "w") as f:
        f.write(er_diagram)
    
    # Generate PNG using graphviz
    import subprocess
    result = subprocess.run(
        ["dot", "-Tpng", dot_path, "-o", output_path + ".png"],
        capture_output=True,
        text=True
    )
    
    if result.returncode == 0:
        # Clean up DOT file
        os.remove(dot_path)
        return True
    else:
        print(f"Error generating diagram: {result.stderr}")
        return False

if __name__ == "__main__":
    if generate():
        print("✅ Generated entity_relationship.png")
    else:
        print("❌ Failed to generate diagram")
