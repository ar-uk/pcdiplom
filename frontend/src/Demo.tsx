import React from 'react'
import { mockComponents, mockCommunityBuilds, mockUsers } from './api/mock/mockData'
import { ComponentCategory } from './types'

export function Demo() {
  // Group components by category
  const cpus = mockComponents.filter(c => c.category === ComponentCategory.CPU).slice(0, 4)
  const gpus = mockComponents.filter(c => c.category === ComponentCategory.GPU).slice(0, 5)

  const formatPrice = (price: number) => `$${price.toLocaleString()}`

  return (
    <div className="min-h-screen bg-gray-900 text-gray-100">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-gray-800 border-b border-gray-700 shadow-lg">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="text-2xl font-bold bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent">
                🖥️ PCBuilder
              </div>
              <span className="ml-4 px-3 py-1 bg-blue-600 text-xs font-bold rounded-full">
                DESIGN PREVIEW
              </span>
            </div>
            <nav className="flex items-center space-x-6">
              <a href="#components" className="hover:text-blue-400 transition">
                Components
              </a>
              <a href="#community" className="hover:text-blue-400 transition">
                Community
              </a>
              <a href="#profile" className="hover:text-blue-400 transition">
                Profile
              </a>
              <a href="#ai" className="hover:text-blue-400 transition">
                AI Assistant
              </a>
            </nav>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="bg-gradient-to-r from-gray-800 to-gray-900 border-b border-gray-700 py-16">
        <div className="max-w-7xl mx-auto px-6 text-center">
          <h1 className="text-5xl font-bold mb-4">
            Build Your Perfect PC Setup
          </h1>
          <p className="text-xl text-gray-400 mb-8">
            Choose from thousands of components and discover community builds
          </p>
          <button className="px-8 py-3 bg-gradient-to-r from-blue-500 to-purple-500 rounded-lg font-bold hover:opacity-90 transition">
            Start Building
          </button>
        </div>
      </section>

      {/* CPUs Section */}
      <section id="components" className="py-16 border-b border-gray-700">
        <div className="max-w-7xl mx-auto px-6">
          <h2 className="text-4xl font-bold mb-2">Component Showcase</h2>
          <p className="text-gray-400 mb-8">Featured CPUs & GPUs from our catalog</p>

          {/* CPUs */}
          <div className="mb-16">
            <h3 className="text-2xl font-bold mb-6 text-blue-400">Processors (CPUs)</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              {cpus.map(cpu => (
                <div
                  key={cpu.id}
                  className="bg-gray-800 border border-gray-700 rounded-lg p-5 hover:border-blue-500 transition group cursor-pointer"
                >
                  <div className="mb-3">
                    <h4 className="font-bold text-lg group-hover:text-blue-400 transition truncate">
                      {cpu.name}
                    </h4>
                    <p className="text-sm text-gray-500">{cpu.manufacturer}</p>
                  </div>

                  {cpu.specs && (
                    <div className="bg-gray-900 rounded p-3 mb-4 text-sm space-y-2">
                      <div className="flex justify-between">
                        <span className="text-gray-400">Cores/Threads:</span>
                        <span className="font-semibold">
                          {(cpu.specs as any).cores}/{(cpu.specs as any).threads}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Base/Boost:</span>
                        <span className="font-semibold">
                          {(cpu.specs as any).baseClockGHz}/{(cpu.specs as any).boostClockGHz} GHz
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">TDP:</span>
                        <span className="font-semibold">{(cpu.specs as any).tdpW}W</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Socket:</span>
                        <span className="font-semibold">{(cpu.specs as any).socket}</span>
                      </div>
                    </div>
                  )}

                  <div className="flex items-center justify-between">
                    <span className="text-2xl font-bold text-green-400">
                      {formatPrice(cpu.price)}
                    </span>
                    <span
                      className={`text-xs px-2 py-1 rounded ${
                        cpu.inStock
                          ? 'bg-green-900 text-green-300'
                          : 'bg-red-900 text-red-300'
                      }`}
                    >
                      {cpu.inStock ? 'In Stock' : 'Out of Stock'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* GPUs */}
          <div>
            <h3 className="text-2xl font-bold mb-6 text-purple-400">Graphics Cards (GPUs)</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6">
              {gpus.map(gpu => (
                <div
                  key={gpu.id}
                  className="bg-gray-800 border border-gray-700 rounded-lg p-4 hover:border-purple-500 transition group cursor-pointer"
                >
                  <div className="mb-3">
                    <h4 className="font-bold text-lg group-hover:text-purple-400 transition truncate">
                      {gpu.name}
                    </h4>
                    <p className="text-sm text-gray-500">{gpu.manufacturer}</p>
                  </div>

                  {gpu.specs && (
                    <div className="bg-gray-900 rounded p-3 mb-4 text-xs space-y-1">
                      <div className="flex justify-between">
                        <span className="text-gray-400">Memory:</span>
                        <span className="font-semibold">{(gpu.specs as any).memory}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Core Clock:</span>
                        <span className="font-semibold">
                          {(gpu.specs as any).boostClockMHz} MHz
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Power:</span>
                        <span className="font-semibold">
                          {(gpu.specs as any).powerRequirementW}W
                        </span>
                      </div>
                    </div>
                  )}

                  <div className="flex items-center justify-between">
                    <span className="text-xl font-bold text-green-400">
                      {formatPrice(gpu.price)}
                    </span>
                    <span
                      className={`text-xs px-2 py-1 rounded ${
                        gpu.inStock
                          ? 'bg-green-900 text-green-300'
                          : 'bg-red-900 text-red-300'
                      }`}
                    >
                      {gpu.inStock ? '✓' : '✗'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Community Builds */}
      <section id="community" className="py-16 border-b border-gray-700">
        <div className="max-w-7xl mx-auto px-6">
          <h2 className="text-4xl font-bold mb-2">Community Builds</h2>
          <p className="text-gray-400 mb-8">
            Explore {mockCommunityBuilds.length} popular builds from the community
          </p>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {mockCommunityBuilds.map(build => (
              <div
                key={build.id}
                className="bg-gray-800 border border-gray-700 rounded-lg overflow-hidden hover:border-blue-500 transition group cursor-pointer"
              >
                {/* Build Header */}
                <div className="bg-gray-900 p-6 border-b border-gray-700">
                  <h3 className="text-xl font-bold mb-2 group-hover:text-blue-400 transition">
                    {build.name}
                  </h3>
                  <p className="text-sm text-gray-400">{build.description}</p>
                </div>

                {/* Build Info */}
                <div className="p-6 space-y-4">
                  {/* Author */}
                  <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 bg-gradient-to-r from-blue-500 to-purple-500 rounded-full flex items-center justify-center text-white font-bold">
                      {build.author.username.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <p className="font-semibold">{build.author.username}</p>
                      <p className="text-xs text-gray-500">Build Creator</p>
                    </div>
                  </div>

                  {/* Components */}
                  <div className="bg-gray-900 rounded p-3">
                    <p className="text-xs text-gray-400 mb-2 font-bold">
                      {build.components.length} COMPONENTS
                    </p>
                    <div className="space-y-1">
                      {build.components.slice(0, 3).map((comp, idx) => (
                        <p key={idx} className="text-sm truncate">
                          • {comp.name}
                        </p>
                      ))}
                    </div>
                  </div>

                  {/* Stats Row */}
                  <div className="grid grid-cols-3 gap-2 text-center text-sm">
                    <div className="bg-gray-900 rounded p-2">
                      <p className="font-bold text-yellow-400">★ {build.averageRating}</p>
                      <p className="text-xs text-gray-500">{build.reviewCount} reviews</p>
                    </div>
                    <div className="bg-gray-900 rounded p-2">
                      <p className="font-bold text-blue-400">👁️ {build.views}</p>
                      <p className="text-xs text-gray-500">views</p>
                    </div>
                    <div className="bg-gray-900 rounded p-2">
                      <p className="font-bold text-red-400">❤️ {build.likes}</p>
                      <p className="text-xs text-gray-500">likes</p>
                    </div>
                  </div>

                  {/* Price & Tags */}
                  <div>
                    <p className="text-2xl font-bold text-green-400 mb-3">
                      {formatPrice(build.estimatedPrice)}
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {build.tags.slice(0, 3).map((tag, idx) => (
                        <span
                          key={idx}
                          className="bg-blue-900 text-blue-300 text-xs px-2 py-1 rounded"
                        >
                          #{tag}
                        </span>
                      ))}
                    </div>
                  </div>

                  {/* View Button */}
                  <button className="w-full mt-4 bg-blue-600 hover:bg-blue-700 transition px-4 py-2 rounded font-bold">
                    View Build Details
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* User Profile Section */}
      <section id="profile" className="py-16 border-b border-gray-700">
        <div className="max-w-7xl mx-auto px-6">
          <h2 className="text-4xl font-bold mb-8">User Profile Preview</h2>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Profile Card */}
            <div className="lg:col-span-1">
              <div className="bg-gray-800 border border-gray-700 rounded-lg p-8 text-center">
                <div className="w-24 h-24 bg-gradient-to-r from-blue-500 to-purple-500 rounded-full mx-auto mb-6 flex items-center justify-center text-4xl">
                  👤
                </div>
                <h3 className="text-2xl font-bold mb-2">{mockUsers[0].username}</h3>
                <p className="text-gray-400 mb-4">{mockUsers[0].email}</p>
                <span className="inline-block bg-blue-900 text-blue-300 px-3 py-1 rounded-full text-sm font-bold mb-6">
                  {mockUsers[0].role}
                </span>
                <button className="w-full bg-blue-600 hover:bg-blue-700 transition px-4 py-2 rounded font-bold mb-3">
                  Edit Profile
                </button>
                <button className="w-full bg-gray-700 hover:bg-gray-600 transition px-4 py-2 rounded font-bold">
                  Settings
                </button>
              </div>
            </div>

            {/* Stats & Activity */}
            <div className="lg:col-span-2 space-y-6">
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-gray-800 border border-gray-700 rounded-lg p-6">
                  <p className="text-gray-400 text-sm mb-2">My Builds</p>
                  <p className="text-4xl font-bold text-blue-400">12</p>
                </div>
                <div className="bg-gray-800 border border-gray-700 rounded-lg p-6">
                  <p className="text-gray-400 text-sm mb-2">Builds Shared</p>
                  <p className="text-4xl font-bold text-purple-400">5</p>
                </div>
                <div className="bg-gray-800 border border-gray-700 rounded-lg p-6">
                  <p className="text-gray-400 text-sm mb-2">Total Likes</p>
                  <p className="text-4xl font-bold text-red-400">346</p>
                </div>
                <div className="bg-gray-800 border border-gray-700 rounded-lg p-6">
                  <p className="text-gray-400 text-sm mb-2">Community Rating</p>
                  <p className="text-4xl font-bold text-yellow-400">4.7★</p>
                </div>
              </div>

              <div className="bg-gray-800 border border-gray-700 rounded-lg p-6">
                <h4 className="font-bold text-lg mb-4">Recent Activity</h4>
                <div className="space-y-3">
                  <p className="text-gray-400">✓ Created "Ultimate Gaming Beast" build</p>
                  <p className="text-gray-400">✓ Shared build with 150 community members</p>
                  <p className="text-gray-400">✓ Received 42 positive reviews</p>
                  <p className="text-gray-400">✓ Reached 3.5K views on latest build</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* AI Assistant Section */}
      <section id="ai" className="py-16 border-b border-gray-700">
        <div className="max-w-7xl mx-auto px-6">
          <h2 className="text-4xl font-bold mb-8">AI Assistant Preview</h2>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Chat Interface */}
            <div className="lg:col-span-2 bg-gray-800 border border-gray-700 rounded-lg overflow-hidden flex flex-col h-96">
              {/* Chat Header */}
              <div className="bg-gray-900 border-b border-gray-700 p-4 flex items-center space-x-3">
                <div className="w-10 h-10 rounded-full bg-gradient-to-r from-green-400 to-cyan-400 flex items-center justify-center text-white font-bold">
                  🤖
                </div>
                <div>
                  <p className="font-bold">PCBuilder AI Assistant</p>
                  <p className="text-xs text-green-400">Online</p>
                </div>
              </div>

              {/* Chat Messages */}
              <div className="flex-1 overflow-y-auto p-4 space-y-4">
                <div className="flex justify-start">
                  <div className="bg-gray-700 rounded-lg p-3 max-w-xs text-sm">
                    <p>Hi! 👋 I'm your AI assistant. I can help you:</p>
                    <p className="text-xs text-gray-300 mt-2">
                      • Find compatible components • Suggest builds • Answer questions about specs
                    </p>
                  </div>
                </div>

                <div className="flex justify-end">
                  <div className="bg-blue-600 rounded-lg p-3 max-w-xs text-sm">
                    <p>What's the best CPU for gaming under $500?</p>
                  </div>
                </div>

                <div className="flex justify-start">
                  <div className="bg-gray-700 rounded-lg p-3 max-w-xs text-sm">
                    <p>
                      Great question! For gaming under $500, I recommend:
                    </p>
                    <p className="text-xs text-gray-300 mt-2">
                      • AMD Ryzen 7 7700X - $299 ✓
                    </p>
                    <p className="text-xs text-gray-300">
                      • Intel Core i7-13700K - $419 ✓
                    </p>
                  </div>
                </div>
              </div>

              {/* Input */}
              <div className="bg-gray-900 border-t border-gray-700 p-4 flex gap-2">
                <input
                  type="text"
                  placeholder="Ask me anything..."
                  className="flex-1 bg-gray-800 border border-gray-700 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
                  readOnly
                />
                <button className="bg-blue-600 hover:bg-blue-700 transition px-4 py-2 rounded font-bold">
                  Send
                </button>
              </div>
            </div>

            {/* Features Panel */}
            <div className="space-y-4">
              <div className="bg-gray-800 border border-gray-700 rounded-lg p-6">
                <h4 className="font-bold text-lg mb-4">🎯 AI Capabilities</h4>
                <div className="space-y-3 text-sm">
                  <div className="flex items-start space-x-2">
                    <span className="text-green-400 font-bold">✓</span>
                    <span>Smart component recommendations</span>
                  </div>
                  <div className="flex items-start space-x-2">
                    <span className="text-green-400 font-bold">✓</span>
                    <span>Compatibility checking</span>
                  </div>
                  <div className="flex items-start space-x-2">
                    <span className="text-green-400 font-bold">✓</span>
                    <span>Budget optimization</span>
                  </div>
                  <div className="flex items-start space-x-2">
                    <span className="text-green-400 font-bold">✓</span>
                    <span>Performance predictions</span>
                  </div>
                  <div className="flex items-start space-x-2">
                    <span className="text-green-400 font-bold">✓</span>
                    <span>24/7 availability</span>
                  </div>
                </div>
              </div>

              <div className="bg-gray-800 border border-gray-700 rounded-lg p-6">
                <h4 className="font-bold text-lg mb-4">💡 Common Questions</h4>
                <div className="space-y-2">
                  <button className="w-full text-left text-sm bg-gray-700 hover:bg-gray-600 transition px-3 py-2 rounded">
                    Best 4K gaming build?
                  </button>
                  <button className="w-full text-left text-sm bg-gray-700 hover:bg-gray-600 transition px-3 py-2 rounded">
                    Budget under $800?
                  </button>
                  <button className="w-full text-left text-sm bg-gray-700 hover:bg-gray-600 transition px-3 py-2 rounded">
                    Streaming setup?
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 border-t border-gray-700 py-8">
        <div className="max-w-7xl mx-auto px-6">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
            <div>
              <h5 className="font-bold mb-4">PCBuilder</h5>
              <p className="text-sm text-gray-400">
                The platform for building and sharing custom PC setups
              </p>
            </div>
            <div>
              <h5 className="font-bold mb-4">Features</h5>
              <ul className="text-sm text-gray-400 space-y-2">
                <li>Builder Tool</li>
                <li>Community Builds</li>
                <li>AI Assistant</li>
              </ul>
            </div>
            <div>
              <h5 className="font-bold mb-4">Resources</h5>
              <ul className="text-sm text-gray-400 space-y-2">
                <li>Documentation</li>
                <li>FAQ</li>
                <li>Support</li>
              </ul>
            </div>
            <div>
              <h5 className="font-bold mb-4">Connect</h5>
              <ul className="text-sm text-gray-400 space-y-2">
                <li>Twitter</li>
                <li>Discord</li>
                <li>GitHub</li>
              </ul>
            </div>
          </div>
          <div className="border-t border-gray-700 pt-6 text-center text-sm text-gray-500">
            <p>&copy; 2024 PCBuilder. All rights reserved. — Design Preview</p>
          </div>
        </div>
      </footer>
    </div>
  )
}
