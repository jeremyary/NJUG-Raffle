@Grab( 'org.codehaus.groovy.modules.http-builder:http-builder:0.5.2' )
import groovyx.net.http.*
import groovy.json.JsonSlurper

// let's look past this for now...we'll circle back round to the secret sauce in a bit
ArrayList.metaClass.getRand = { number ->
    (0..<(number?:1)).collect{delegate[new Random().nextInt(delegate.size())]}
}

// and we need some way to recursively apply the secret sauce and assure no duplicate winners, no?
def winners = []
def getWinner
getWinner = { entryList ->
    def winner = entryList.getRand().member.name
    // no repeat winners, cheaters!
    if (winners.contains(winner)) {
        getWinner(entryList)
    } else {
        winners.add(winner)
        println "...and the raffle winner is ${winner}!"

        // since we're not allowing dups, make sure we still have names left in the hat to draw from
        if (winners.size() == entryList.size()) {
            println "looks like everyone's a winner today! Thanks for playing!"
        } else {
            print  'Another drawing for this event? [y/n]: '
            System.in.withReader {
                if (it.readLine() == "y") {
                    getWinner(entryList)
                }
            }
        }
    }
}

// user-specific API key...hey! Get your own!
def API_KEY = '474c55605216352577306a725743325'

// top-level API url
def http = new HTTPBuilder( 'http://api.meetup.com' )

// first request, let's fetch all the events for the group with the url name "nashvillejug"
http.get( path: '/events',
    query: [
        key: API_KEY,
        sign: true,
        page: 20,
        group_urlname: 'nashvillejug',
	status: 'past'
    ]) { eventsResponse, eventsJson ->

    def events = new JsonSlurper().parseText(eventsJson.toString())
    println "${events.meta.count} event(s) found..."

    // ok, so we have some events...let's see how many faithful JUG members RSVP'd to enter
    events.results[-1].with {
        println name
        println "${rsvpcount} people RSVP'd to attend..."

        http.get( path: '/2/rsvps',
                query: [
                        key: API_KEY,
                        sign: true,
                        page: 20,
                        rsvp: 'yes',
                        event_id: id
                ]) { rsvpResponse, rsvpJson ->

            def rsvps = new JsonSlurper().parseText(rsvpJson.toString())
            rsvps.results.each {
                // and the final contestants are *drumroll*
                print "${it.member.name}, "
            }

            // OK, Johnny, tell us who won!
            getWinner(rsvps.results)
        }

    }
}

